package clientAPI;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Formatter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class Mythread extends Thread {
	public static  Socket m_socket = null;
	public static CallBack m_cb = null;
	
	public Mythread(Socket socket) {
		m_socket = socket;
		
	}
	
	public void setCallBack(CallBack cb) {
		m_cb = cb;
	}
	
	
	// 收包处理函数 循环处理服务器发送过来的消息
	// 消息结构 消息头(14字节)+消息体(json格式) 
	// 消息头 长度（4字节）（10 + 消息体长度）+ 校验(4字节 目前没用到) + 消息号(2字节) + 版本号(1字节) + 快捷校验(1字节) + retcode（2字节）

	public void run() {
		super.run();
		try {
			InputStream in = m_socket.getInputStream();
            byte[] buffer = new byte[65535];
            int len = -1;
            int left = 4;
            int right = 0;
            boolean gHead = true;
            short commond = 0;
            //System.out.println("recv begin");
            while (left > 0) {
            	len = Trans.read(in, buffer, right, left);
            	//System.out.println("recv len " + len);
            	left -= len;
            	right += len;
            	if (left != 0) {
            		continue;
            	}
            	if (gHead) {
            		left = Trans.Byte2Int(buffer);
            		right = 0;
            		gHead = false;
            	}else {
            		commond = Trans.Byte2Short(Arrays.copyOfRange(buffer, 4, 6));
            		JSONObject jsonObject = JSON.parseObject(new String(Arrays.copyOfRange(buffer, 10, right)));
            		LOG("recvmsg commond %3d %s ", commond, jsonObject);
            		switch (commond){
	                    case 301: msg301(jsonObject); break;
	                    case 302: msg302(jsonObject); break;
	                    case 102: msg102(jsonObject); break;
	                    case 104: msg104(jsonObject); break;
	                    case 901: msg901(jsonObject); break;
	                    case 9999: break;
	                    default: LOG("error commond %d", commond);
	                }
            		left = 4;
            		right = 0;
            		gHead = true;
            	}
            	
            }
        }catch (IOException e){
            e.printStackTrace();
        }
	}
	
	
    int msg301(JSONObject jsonObject){
    	// 处理服务器登录返回包，并与主线程同步
        int result = 0;
        if (jsonObject.containsKey("result") ){
            result = (int) jsonObject.get("result");
        }
        String randomSeed = "";
        if (result != 1){
            LOG("msg: %s result: %d", jsonObject.get("msg"), result); 
        }else {
            if (jsonObject.containsKey("randomSeed")){
                randomSeed = (String) jsonObject.get("randomSeed");
            }
            //LOG("randomSeed: %s, result %d", randomSeed, result); 
        }
        ClientAPi.getInstance().setSeed(randomSeed, result);
        return 0;
    }

    int msg302(JSONObject jsonObject){
    	// 处理服务器验证返回包，并与主线程同步
        int result = 0;
        if (jsonObject.containsKey("result")){
            result = (int) jsonObject.get("result");
        }
        ClientAPi.getInstance().setAuth(result);
        if (result != 1){
            LOG("msg: %s result: %d", jsonObject.get("msg"), result);
        }else {
            //LOG("auth result:%d", result);
        }
        return 0;
    }

    int msg102(JSONObject jsonObject){
    	// 处理用户登录返回包，并与主线程同步
        int result = 0;
        if (jsonObject.containsKey("result") ){
            result = (int) jsonObject.get("result");
        }
        int uid = 0;
        String proId = "";
        if (result != 1){
            LOG("msg: %s, result: %d", jsonObject.get("msg"), result);
        }else {
            if (jsonObject.containsKey("uid")){
                uid = (int) jsonObject.get("uid");
            }
            if (jsonObject.containsKey("seq")){
            	proId = (String) jsonObject.get("seq");
            }
            //LOG("login uid: %d , result: %d", uid, result); 
        }
        ClientAPi.getInstance().setUid(uid, result, proId);
        return 0;
    }

    int msg104(JSONObject jsonObject){
    	// 处理用户登出返回包
        int result = 0;
        if (jsonObject.containsKey("result")){
            result = (int) jsonObject.get("result");
        }
        int uid = 0;
        String seq = "";
        if (result != 1){
            LOG("msg: %s, result %d", jsonObject.get("msg"), result); 
        }else {
            if (jsonObject.containsKey("uid")){
                uid = (int) jsonObject.get("uid");
            }
            if (jsonObject.containsKey("seq")){
                seq = (String) jsonObject.get("seq");
            }
            //LOG("logout uid: %d, seq: %s", uid, seq); 
        }
        return 0;
    }

    int msg901(JSONObject jsonObject){
    	// 处理用户交互返回消息，调用回调函数处理
        int result = 0;

        if (jsonObject.containsKey("result")){
            result = (int) jsonObject.get("result");
        }
        if (result != 1){
            LOG("msg: %s, result %d", jsonObject.get("msg"), result); 
            return -1;
        }else {
            m_cb.callBack(jsonObject);
            //LOG("aichat result: %d", result);
        }
        return 0;
    }

    public static void LOG(String format, Object... args) {
    	System.out.println(new Formatter().format(format, args).toString());
    }
}
