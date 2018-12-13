package clientAPI;

public class Info implements Cloneable{

	private String appId;
	private String appKey;
	private int type;
	private String ip;
	private int port;
	private char version;
	private char magic;
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
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public char getVersion() {
		return version;
	}
	public void setVersion(char version) {
		this.version = version;
	}
	public char getMagic() {
		return magic;
	}
	public void setMagic(char magic) {
		this.magic = magic;
	}
	@Override
	public Object clone() {
		Info addr = null;
		try{
			addr = (Info)super.clone();
		}catch(CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return addr;
	}
	
	
}
