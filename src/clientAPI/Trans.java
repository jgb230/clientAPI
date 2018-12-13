package clientAPI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.Map;

import com.alibaba.fastjson.JSON;

public class Trans {

    public static final String CHARCODE = "utf-8";
    public static Info m_info = new Info();
    public static void init(Info info) {
    	m_info = info;
    }
    
    public static int read(InputStream in, byte[] buffer, int position, int len) throws IOException {
    	byte[] temBuf = new byte[len];
    	
    	int ret = 0;
    	try {
    		int readLen = in.read(temBuf);
        	for (int i = 0; i < readLen; i++) {
        		buffer[position + i] = temBuf[i];
        	}
            ret = readLen;
    	}catch (java.net.SocketTimeoutException e) {
    		ClientAPi.getInstance().sendHeartBeat();
    		ret = 0;
    	}catch (java.net.SocketException e) {
			ret = ClientAPi.getInstance().reconnect();
		}catch(IOException e) {
			e.printStackTrace();
			ret = -1;
		}
    	return ret;

    }
    
    public static int wirte(OutputStream socketOut, short commond, Map<String, Object> map){
    	
    	byte[] head = Trans.buildHead(commond);
    	LOG("sendmsg %s",map.toString());
    	String msg = JSON.toJSONString(map);
    	int length = 0;
		try {
			length = 10 + msg.getBytes(CHARCODE).length;
			setInt(length, head, 0);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	int ret = 0;
    	try {
    		LOG("head %s ", byteArrayToHexString(head));
    		socketOut.write(head);
            socketOut.write(msg.getBytes(CHARCODE));
            socketOut.flush();
            ret = length;
		}catch (java.net.SocketTimeoutException e) {
			try {
				ClientAPi.getInstance().sendHeartBeat();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		ret = 0;
		}catch (java.net.SocketException e) {
			//reconnect 
			ret = 0;
		}catch(IOException e) {
			e.printStackTrace();
			ret = -1;
		}
    	return ret;
    }
    
    public static final String bytesToHexString(byte[] bArray, int len) {
          StringBuffer sb = new StringBuffer(bArray.length);
          String sTemp;
          for (int i = 0; i < len; i++) {
           sTemp =Integer.toHexString(0xFF & bArray[i]);
           if (sTemp.length() < 2)
        	   sb.append(0);
           sb.append(sTemp.toUpperCase());
          }
          return sb.toString();
     }

  
    
    public static byte[] buildHead(short commond) {
    	byte [] head = new byte[14];
    	setInt(0, head, 0);
    	setInt(0, head, 4);
    	setShort(commond, head, 8);
    	setChar(m_info.getVersion(), head, 10);
    	setChar(m_info.getMagic(), head, 11);
    	setShort((short)0, head, 12);
    	return head;
    }
    
    private static void setChar(char c, byte[] head, int begin) {
    	char[] cc = new char[1];
    	cc[0] = c;
    	byte[] bb = getBytes(cc);
    	for (int i = 0; i < bb.length; i++) {
    		head[begin + i] = bb[i];
    	}
	}
    
    private static void setShort(short commond, byte[] head, int begin) {	
    	byte[]bytes = ShortToByte(commond);
    	for (int i = 0; i < bytes.length; ++i) {
    		head[begin + i] = bytes[i];
    	}
	}
	public static void setInt(int num, byte [] head, int begin) {
    	byte[]bytes = IntToByte(num);
    	for (int i = 0; i < bytes.length; ++i) {
    		head[begin + i] = bytes[i];
    	}
    }
    
    public static int Byte2Int(byte[]bytes) {
		return (bytes[3]&0xff)<<24
			| (bytes[2]&0xff)<<16
			| (bytes[1]&0xff)<<8
			| (bytes[0]&0xff);
	}
    
    public static byte[] IntToByte(int num){
		byte[]bytes=new byte[4];
		bytes[3]=(byte) ((num>>24)&0xff);
		bytes[2]=(byte) ((num>>16)&0xff);
		bytes[1]=(byte) ((num>>8)&0xff);
		bytes[0]=(byte) (num&0xff);
		return bytes;
    }
    
    public static short Byte2Short(byte[]bytes) {
		return (short) ( (bytes[1]&0xff)<<8
			| (bytes[0]&0xff));
	}
    
    public static byte[] ShortToByte(short num){
		byte[]bytes=new byte[2];
		bytes[1]=(byte) ((num>>8)&0xff);
		bytes[0]=(byte) (num&0xff);
		return bytes;
    }
    
    private static byte[] getBytes (char[] chars) {
	   Charset cs = Charset.forName(CHARCODE);
	   CharBuffer cb = CharBuffer.allocate(chars.length);
	   cb.put(chars);
	   cb.flip();
	   ByteBuffer bb = cs.encode(cb);
	   return bb.array();
	 }


	private static char[] getChars (byte[] bytes) {
      Charset cs = Charset.forName(CHARCODE);
      ByteBuffer bb = ByteBuffer.allocate(bytes.length);
      bb.put(bytes);
      bb.flip();
      CharBuffer cb = cs.decode(bb);
	   return cb.array();
	}
	
	
	private static final String hexDigIts[] = {"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};

    public static String MD5Encode(String origin){
        String resultString = null;
        try{
            resultString = new String(origin);
            MessageDigest md = MessageDigest.getInstance("MD5");
            if(null == CHARCODE || "".equals(CHARCODE)){
                resultString = byteArrayToHexString(md.digest(resultString.getBytes()));
            }else{
                resultString = byteArrayToHexString(md.digest(resultString.getBytes(CHARCODE)));
            }
        }catch (Exception e){
        }
        return resultString;
    }


    public static String byteArrayToHexString(byte b[]){
        StringBuffer resultSb = new StringBuffer();
        for(int i = 0; i < b.length; i++){
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }

    public static String byteToHexString(byte b){
        int n = b;
        if(n < 0){
            n += 256;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigIts[d1] + hexDigIts[d2];
    }

    public static void LOG(String format, Object... args) {
    	System.out.println(new Formatter().format(format, args).toString());
    }
}
