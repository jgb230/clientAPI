package clientAPI;


import java.io.IOException;
import java.util.Formatter;

import clientAPI.ClientAPi;
import clientAPI.Info;

public class javaDemo_main {

	public static void main(String[] args) throws IOException, InterruptedException {
		// 初始化回调函数，在javaDemo_myCallBack中自定义交互消息处理流程
		javaDemo_myCallBack mycb = new javaDemo_myCallBack();

		String appId = "4.00002";
		String appKey = "!4j7oTLOXIKOFW@P";
		int type = 1;
	    char version = 0x01;
	    Info info = new Info();
	    info.addAppId_key(new App(appId, appKey, type));
	    info.setIp("172.16.0.27");
	    info.setPort(2345);
	    info.setVersion(version);
	    info.setMagic('$');

	    int ret = 0;
	    // 初始化client，包含服务器登录和验证，心跳包发送, 
	    ret = ClientAPi.getInstance().initClient(info, mycb);
	    if (ret != 0 ){
	        LOG("init faile");
	        return;
	    }
	    
	    LOG("set callbak！\n" ,ret);
	    
	    String proid = "jgbtest";
	    String msg = "你好";
	    // 用户登录
	    int [] aid = new int[1];
	    ret = ClientAPi.getInstance().login(appId, proid, aid);
	    if (ret != 0){
	        LOG("login faile ret:%d", ret);
	    }
	    int uid = aid[0];
	    LOG("====udi : %d proid %s" ,uid , proid);
	    
	    // 发送交互消息
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
	    	// 用户登出
			ClientAPi.getInstance().logout(appId, proid, uid);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    // 多用户多线程登录
	    javaDemo_TestThread [] mt = new javaDemo_TestThread[5];
	    for (int i = 0; i < 5; i++) {
	    	mt[i] = new javaDemo_TestThread(i, appId);
	    }
	    for (int i = 0; i < 5; i++) {
	    	mt[i].start();
	    }
	    
	    
	}
	public static void LOG(String format, Object... args) {
    	System.out.println(new Formatter().format(format, args).toString());
    }
}
