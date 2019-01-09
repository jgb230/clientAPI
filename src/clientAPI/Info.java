package clientAPI;

import java.util.Vector;

public class Info implements Cloneable{

	private Vector<App> appId_key = new Vector<App>();
	
	private String ip;
	private int port;
	private char version;
	private char magic;
	
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
	
	public Vector<App> getAppId_key() {
		return appId_key;
	}
	
	public void addAppId_key(App app) {
		this.appId_key.add(app);
	}
	
	
}
