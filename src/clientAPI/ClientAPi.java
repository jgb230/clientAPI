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
	
	public static  Socket socket = null;
	public static InputStream in = null;
    public static final String CHARCODE = "utf-8";
    public static OutputStream socketOut = null;
    
    public static int m_uid;
    public static String m_randomSeed;
    public static Mythread mt = null;
    public static int m_result;
    public static long m_heartTime = 0;
    
    private Map<Integer, String> m_loginId = new ConcurrentHashMap<>();
    public static RWLock m_rwLock = new RWLock();

    
    public Lock m_slock = new ReentrantLock(); 
    public Condition m_scond = m_slock.newCondition();
    
    private Info m_info = new Info();
    
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
            // 得到socket连接
            socket = new Socket(m_info.getIp(), m_info.getPort());
            socket.setSoTimeout(5000);
            System.out.println("socket sucess");
            in= socket.getInputStream();
            socketOut = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public int reconnect() {
    	int time = 0;
    	while (time >= 3) {
    		try {
        		socket = new Socket(m_info.getIp(), m_info.getPort());
        		for (int key : m_loginId.keySet()) {
        			int [] aid = new int[1];
        			login(m_loginId.get(key), aid);
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
    	mt = new Mythread(socket);
    	mt.start();
    }
    
    public int initClient(Info info) {
    	if(info.getAppId().isEmpty() || 
    	   info.getAppKey().isEmpty() ||
    	   info.getIp().isEmpty() ||
    	   info.getPort() == 0 ||
    	   info.getVersion() == 0 ||
    	   info.getMagic() != '$') {
    		LOG(" client info erro!");
    		return -1;
    	}
    	m_info = (Info)info.clone();
    	Trans.init(m_info);
    	
    	ClientAPi.getInstance().SocketData();
    	ClientAPi.getInstance().start();
    	int ret = ClientAPi.getInstance().servLogin(m_info.getAppId(), m_info.getType());
        if (ret != 0){
            LOG("servLogin error! errno:%d", ret);
            return ret;
        }
        ret = ClientAPi.getInstance().servAuth(m_info.getAppId(), m_info.getAppKey());
        if (ret != 0){
            LOG("servAuth error! errno:%d", ret);
            return ret;
        }
        return ret;
    }
    
    
    public int sendMsg(int uid, String msg){
        return ClientAPi.getInstance().aichat(uid, msg, m_info.getAppId());
    }

    public int login(String proId, int[] Mid){
        return ClientAPi.getInstance().login(m_info.getAppId(), proId, Mid);
    }

    public int logout(String proId, int uid) throws InterruptedException{
        return ClientAPi.getInstance().logout(m_info.getAppId(), proId, uid);
    }
    
    public void setRecvHandler(CallBack cb) {
    	mt.setCallBack(cb);
    }

    public int servLogin(String appId, int type){
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

    public int login(String appId, String proId, int[] Mid){

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
