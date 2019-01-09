package clientAPI;

public class App implements Cloneable {
	App(String appid, String appkey, int tp){
		setAppId(appid);
		setAppKey(appkey);
		setType(tp);
	}
	private String appId;
	private String appKey;
	private int type;
	public String getAppId() {
		return appId;
	}
	public void setAppId(String appId) {
		this.appId = appId;
	}
	public String getAppKey() {
		return appKey;
	}
	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
}
