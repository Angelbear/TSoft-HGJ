package cn.edu.tsinghua.hpc.tmms;

import android.content.Context;
import android.util.Config;
import android.util.Log;

public class UserManager {
	
	protected String mSessionID = null;
	protected UserInfo mUser = null;
	protected Context mContext = null;
	protected RequestLogin mLogin = null;
	
	private static final String TAG = "UserManager";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;	
	private static UserManager mInstance = null;
	
	public UserManager(Context context) {
		assert(context != null);
		mContext = context;
		mLogin = new RequestLogin(mContext);
		mUser = new UserInfo();
	}

	public static UserManager newInstance(Context context) {
	    if (LOCAL_LOGV) {
	    	Log.v(TAG, "UserManager.newInstance()");
	    }
	    mInstance = new UserManager(context);
	    return mInstance;
	}

	public static UserManager getInstance() {
        return mInstance;
	}
	
	// uid must be not null.
	public void setUser(UserInfo user)
	{
		assert(user != null);
		
		mUser = user;		
		return;
	}
	
	public UserInfo getUser() {
		return mUser;
	}
	
	public void setToken(String token) {
		assert(token != null);
		
		mUser.mToken = token;
		return;
	}
	
	public String getToken()
	{		
		return mUser.mToken;
	}

	public void setUserID(String uid) {
		assert(uid != null);
		
		mUser.mUserID = uid;
		return;
	}

	public String getUserID()
	{		
		return mUser.mUserID;
	}
	
	public void setTotal(int total) {
		assert(total >= 0);
		
		mUser.mTotal = total;
		return;
	}
	
	public int getTotal()
	{		
		return mUser.mTotal;
	}
	
	// sessionID must be not null.
	public void setSessionID(String sessionID)
	{
		assert(sessionID != null);
		mSessionID = sessionID;		
	}

	public String getSessionID()
	{
		return mSessionID;
	}
		
	public boolean requestLogin(OnLoginListener listener) {
		return mLogin.login(listener);
	}
	
	public boolean cancelLogin() {
		return mLogin.cancel();
	}
}
