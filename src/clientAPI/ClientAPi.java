package clientAPI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientAPi{
	
	public static Socket socket = null; // 收发数据socket连接
	public static InputStream in = null; // socket输入流
    public static OutputStream socketOut = null; // socket输出流
    
    public static final String CHARCODE = "utf-8"; // 传输同一utf-8编码

    public static Mythread mt = null;  // 异步接受消息线程
    public static int m_result;  // 消息返回码
    public static long m_heartTime = 0; // 心跳消息包
    
    private Map<Integer, String> m_loginId = new ConcurrentHashMap<>(); // 登录用户列表
    public static RWLock m_rwLock = new RWLock(); // 登录用户列表读写锁

    public Lock m_slock = new ReentrantLock(); // 账号验证同步锁
    public Condition m_scond = m_slock.newCondition(); // 账号验证同步条件变量
    public static String m_randomSeed; // 账号验证种子

    private Info m_info = new Info(); // 初始化信息类
    
    private static ClientAPi instance=null;
    private ClientAPi(){
        
    }
    public static synchronized ClientAPi getInstance(){
        if(instance==null){
            instance=new ClientAPi();
        }
        return instance;
    }
    
    public void SocketData() {
        try {
            // 得到socket连接，设置5秒超时
            socket = new Socket(m_info.getIp(), m_info.getPort());
            socket.setSoTimeout(5000);
            LOG("socket sucess");
            in= socket.getInputStream();
            socketOut = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public int reconnect() {
    	// socket重连，重连4次失败，返回失败，成功重新登录所有已登录用户
    	int time = 0;
    	while (time <= 3) {
    		try {
        		socket = new Socket(m_info.getIp(), m_info.getPort());
        		String appId = "";
            	String appKey = "";
            	int type = 0;
            	int ret = 0;
            	for (App ap:m_info.getAppId_key()) {
            		appId = ap.getAppId();
            		appKey = ap.getAppKey();
            		type = ap.getType();
            		ret = ClientAPi.getInstance().servLogin(appId, type);
                    if (ret != 0){
                        LOG("servLogin error! errno:%d", ret);
                        return ret;
                    }
                    // 服务器验证
                    ret = ClientAPi.getInstance().servAuth(appKey, appKey);
                    if (ret != 0){
                        LOG("servAuth error! errno:%d", ret);
                        return ret;
                    }
            	}
        		return 0;
        	}catch(IOException e){
        		 e.printStackTrace();
        		 time++;
        	}
    	}
    	return -1;
    	
    }
    
    public void sendHeartBeat() throws IOException {
    	// 发送心跳包，保持连接
    	long nowTime = new Date().getTime();
    	if (nowTime - m_heartTime < 30) {
    		return;
    	}
    	m_heartTime = nowTime;
    	Map<String, Object> map = new HashMap<String, Object>();
		map.put("nowTime", m_heartTime);
    	Trans.wirte(socketOut, (short)9999, map);
    }
    
    public void start() {
    	// 启动接收消息线程
    	mt = new Mythread(socket);
    	mt.start();
    }
    
    public int initClient(Info info, CallBack cb) {
    	// 初始化消息客户端，启动接收消息线程，服务器登录并验证
    	if(info.getAppId_key().size() == 0 || 
    	   info.getAppId_key().firstElement().getAppId().length() == 0 || 
    	   info.getAppId_key().firstElement().getAppKey().length() == 0 ||
    	   info.getIp().isEmpty() ||
    	   info.getPort() == 0 ||
    	   info.getVersion() == 0 ||
    	   info.getMagic() != '$') {
    		LOG(" client info erro!");
    		return -1;
    	}
    	m_info = (Info)info.clone();
    	Trans.init(m_info);
    	
    	// 启动接收消息线程
    	ClientAPi.getInstance().SocketData();
    	ClientAPi.getInstance().start();
    	// 服务器登录
    	String appId = "";
    	String appKey = "";
    	int type = 0;
    	int ret = 0;
    	for (App ap:m_info.getAppId_key()) {
    		appId = ap.getAppId();
    		appKey = ap.getAppKey();
    		type = ap.getType();
    		ret = ClientAPi.getInstance().servLogin(appId, type);
            if (ret != 0){
                LOG("servLogin error! errno:%d", ret);
                return ret;
            }
            // 服务器验证
            ret = ClientAPi.getInstance().servAuth(appKey, appKey);
            if (ret != 0){
                LOG("servAuth error! errno:%d", ret);
                return ret;
            }
    	}
    	
        
        // 设置服务器返回的交互消息的回调函数cb
    	mt.setCallBack(cb);
        return ret;
    }
    
    
    public int sendMsg(String appId, int uid, String msg){
    	// 向服务器发送uid用户的交互信息msg
        return ClientAPi.getInstance().aichat(uid, msg, appId);
    }

    public int servLogin(String appId, int type){
    	// 服务器appId账号以type形式登录， 与接收消息线程保持同步
    	Map<String, Object> map = new HashMap<String, Object>();
		map.put("servType", type);
		map.put("appid", appId);
		int ret = 0;
		ret = Trans.wirte(socketOut, (short)201, map);

        if (ret < 0){
            LOG("send mes errno : %d",ret);
            return ret;
        }else if( ret == 0){
            LOG("send socket close ");
            return -1;
        }
        m_slock.lock();
		try {
			m_scond.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ret = m_result;
		m_slock.unlock();

        return ret == 1? 0: -1;
    }   

   
    public int servAuth(String appId,String appKey){
    	// 服务器appId账号appKey验证,md5加密服务器登录时返回的种子+appKey， 与接收消息线程保持同步
        String data = m_randomSeed + appKey;
		String sign = Trans.MD5Encode(data);

        Map<String, Object> map = new HashMap<String, Object>();
		map.put("sign", sign);
		map.put("appid", appId);
        
        int ret = 0;
		ret = Trans.wirte(socketOut, (short)202, map);
        if (ret < 0){
            LOG("send mes  errno : %d",ret);
            return ret;
        }else if( ret == 0){
            LOG("send socket close ");
            return -1;
        }

		m_slock.lock();
		try {
			m_scond.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ret = m_result;
		m_slock.unlock();
		
        return ret == 1? 0: -1;
    }   


    public int aichat(int uid,String msg,String appId){
        // 向服务器发送appId下uid用户的交互信息msg， 与接收消息线程异步
        Map<String, Object> map = new HashMap<String, Object>();
		map.put("uid", uid);
		map.put("content", msg);
		map.put("appid", appId);
		
        int ret = Trans.wirte(socketOut, (short)801, map);
        if (ret < 0){
            LOG("send mes  errno : %d",ret);
            return ret;
        }else if( ret == 0){
            LOG("send socket close ");
            return -1;
        }
        
        return 0;
    }   

    public int login( String appId, String proId, int[] Mid){
    	// appId下proId用户登录，返回的uid放入Mid返回，与接收消息线程保持同步
        Map<String, Object> map = new HashMap<String, Object>();
		map.put("uname", proId);
		map.put("passwd", "");
		map.put("keytp", "openid");
		map.put("appid", appId);
		map.put("seq", proId);
		
        int ret = Trans.wirte(socketOut, (short)2, map);
        if (ret < 0){
            LOG("send mes errno : %d" ,ret);
            return ret;
        }else if( ret == 0){
            LOG("send socket close ");
            return -1;
        }
        int tmpid = 0;
        try {
        	do{
            	m_rwLock.lock_read();
                tmpid = recvedUid(proId);
                ret = m_result;
                m_rwLock.release_read();
            }while ( tmpid == 0 );
        }catch (Exception e) {
        	LOG("----------login");
        	e.printStackTrace();
        }
        
        Mid[0] = tmpid;
        return ret == 1? 0:ret ;
    }

    int recvedUid(String proId){
    	for(Object key: m_loginId.keySet()){
            if(m_loginId.get(key).equals(proId)){
                return (int) key;
            }
        }
        return 0;
    }
    public int logout(String appId, String proId, int uid) throws InterruptedException{
    	// 发送proid用户登出消息，并从已登录列表删除
        Map<String, Object> map = new HashMap<String, Object>();
		map.put("uid", uid);
		map.put("appid", appId);
		map.put("seq", proId);
        
		int ret = Trans.wirte(socketOut, (short)4, map);
        if (ret < 0){
            LOG("send mes errno : %d" ,ret);
            return ret;
        }else if( ret == 0){
            LOG("send socket close ");
            return -1;
        }
        m_rwLock.lock_read();
        m_loginId.remove(uid);
        m_rwLock.release_read();
        return 0;
    }

    public void setUid(int uid, int result, String proId){
    	
    	try {
			m_rwLock.lock_write();
			m_result = result;
	        m_loginId.put(uid, proId);
	    	m_rwLock.release_write();
    	} catch (InterruptedException e) {
			LOG("----------setUid");
			e.printStackTrace();
		}

    }

    public void setSeed(String seed, int result){
		m_slock.lock();
		m_randomSeed = seed;
		m_result = result;
		m_scond.signalAll();
		m_slock.unlock();
    }

    public void setAuth(int result){
		m_slock.lock();
		m_result = result;
		m_scond.signalAll();
		m_slock.unlock();
    }

    public static void LOG(String format, Object... args) {
    	System.out.println(new Formatter().format(format, args).toString());
    }
    
    
    
}
