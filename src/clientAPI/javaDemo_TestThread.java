package clientAPI;
import java.util.Formatter;

import clientAPI.ClientAPi;

public class javaDemo_TestThread extends Thread {
	int m_i;
	String appId;
	javaDemo_TestThread(int i, String appid){
		m_i = i;
		appId = appid;
	}
	public void run() {
		String proid = "jgbtest" + String.valueOf(m_i);
	    String msg = "你好";
	    int [] aid = new int[1];
	    int ret = ClientAPi.getInstance().login(appId, proid, aid);
	    if (ret != 0){
	        LOG("login faile ret:%d", ret);
	    }
	    int uid = aid[0];
	    LOG("====udi : %d proid %s" ,uid , proid);
	    
	    for (int i = 0; i < 2; i++){
	        ret = ClientAPi.getInstance().sendMsg(appId, uid, msg);
	        if (0 != ret){
	            LOG("send mes error: %s errno : %d\n" ,ret);
	        }
	        try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    try {
			ClientAPi.getInstance().logout(appId, proid, uid);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void LOG(String format, Object... args) {
    	System.out.println(new Formatter().format(format, args).toString());
    }
}
