package clientAPI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientAPi{
	
	public static  Socket socket = null;
	public static InputStream in = null;
    public static final int port = 2345;
    public static final String ip = "172.16.0.27";
    public static final String CHARCODE = "utf-8";
    public static OutputStream socketOut = null;
    
    public static int m_uid;
    public static String m_randomSeed;
    public static Mythread mt = null;
    public static int m_result;
    
    public static String m_appId = "";
    public static String m_appKey = "";
    public static int m_type = 1;
    
    public Lock m_ulock = new ReentrantLock();  
    public Condition m_ucond = m_ulock.newCondition();
    
    public Lock m_slock = new ReentrantLock(); 
    public Condition m_scond = m_slock.newCondition();
    
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
            socket = new Socket(ip, port);
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
        		socket = new Socket(ip, port);
        		return 0;
        	}catch(IOException e){
        		 e.printStackTrace();
        		 time++;
        	}
    	}
    	return -1;
    	
    }
    
    public void sendHeartBeat() throws IOException {
    	Map<String, Object> map = new HashMap<String, Object>();
		map.put("nowTime", 0);
		    	
    	Trans.wirte(socketOut, (short)9999, map);
    }
    
    public void start() {
    	mt = new Mythread(socket);
    	mt.start();
    }
    
    public int initClient(String appId, String appKey, int type) {
    	
    	m_appId = appId;
        m_appKey = appKey;
        m_type = type;
        
    	ClientAPi.getInstance().SocketData();
    	ClientAPi.getInstance().start();
    	int ret = ClientAPi.getInstance().servLogin(m_appId, m_type);
        if (ret != 0){
            LOG("servLogin error! errno:%d", ret);
            return ret;
        }
        ret = ClientAPi.getInstance().servAuth(m_appId, m_appKey);
        if (ret != 0){
            LOG("servAuth error! errno:%d", ret);
            return ret;
        }
        return ret;
    }
    
    public int initClient(String appId, String appKey) {
    	m_appId = appId;
        m_appKey = appKey;
        m_type = 1;
    	ClientAPi.getInstance().SocketData();
    	ClientAPi.getInstance().start();
    	int ret = ClientAPi.getInstance().servLogin(m_appId, m_type);
        if (ret != 0){
            LOG("servLogin error! errno:%d", ret);
            return ret;
        }
        ret = ClientAPi.getInstance().servAuth(m_appId, m_appKey);
        if (ret != 0){
            LOG("servAuth error! errno:%d", ret);
            return ret;
        }
        return ret;
    }
    
    public int sendMsg(int uid, String msg){
        return ClientAPi.getInstance().aichat(uid, msg, m_appId);
    }

    public int login(String proId, int[] Mid){
        return ClientAPi.getInstance().login(m_appId, proId, Mid);
    }

    public int logout(String proId, int uid){
        return ClientAPi.getInstance().logout(m_appId, proId, uid);
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
        
        m_ulock.lock();
        try {
			m_ucond.await();
			ret = m_result;
			Mid[0] = m_uid;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_ulock.unlock();

        return ret == 1? 0:ret ;
    }

    public int logout(String appId, String proId, int uid){
    	
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
        
        return 0;
    }

    public void setUid(int uid, int result){
    	
    	m_ulock.lock();
        m_uid = uid;
		m_result = result;
		m_ucond.signalAll();
		m_ulock.unlock();

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
