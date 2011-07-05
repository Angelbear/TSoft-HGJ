package cn.edu.tsinghua.hpc.tmms;


public abstract interface OnLoginListener {

	public static final int INIT_LOGIN_COMPLETED = 0;
	public static final int INIT_LOGIN_FAILED = -1;
	public static final int REQUEST_LOGIN_COMPLETED = 0;	
	public static final int REQUEST_LOGIN_FAILED = -2;
	public static final int REQUEST_LOGIN_INTERRUPTED = -3;
	public static final int REQUEST_LOGIN_CANCED = -4;
	public static final int SERVER_AUTHEN_COMPLETED = 0;
	public static final int SERVER_AUTHEN_FAILED = -5;
	
//	public abstract void OnLoginCompleted(UserInfo user);
	public abstract void OnLogin(UserInfo user);
	public abstract void OnLoginFailed(int status, String msg);
		
}
