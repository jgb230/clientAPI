package clientAPI;


import java.util.Formatter;

import com.alibaba.fastjson.JSONObject;

import clientAPI.CallBack;

public class javaDemo_myCallBack implements CallBack{
	@Override
	public void callBack(JSONObject jsonObject) {
		LOG("callback %s ", jsonObject);
	}
	public static void LOG(String format, Object... args) {
    	System.out.println(new Formatter().format(format, args).toString());
    }
}
