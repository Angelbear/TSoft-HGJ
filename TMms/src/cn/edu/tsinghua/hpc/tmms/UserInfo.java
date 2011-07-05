/**
 * 
 */
package cn.edu.tsinghua.hpc.tmms;

/**
 * @author u
 *
 */
public class UserInfo {
	public String mUserID = null;
	public String mToken = null;	
	public int mTotal = 0;
	
	public UserInfo(String uid, String token, int total) {
		mUserID = uid;
		mToken = token;
		mTotal = total;
	}
	
	public UserInfo() {
	}
	
	//add by chenqiang
	public UserInfo(String uid, String token) {
		mUserID = uid;
		mToken = token;
	}
}
