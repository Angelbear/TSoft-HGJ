package cn.edu.tsinghua.hpc.tmms.util;

public class TMmsSms2 {
	
    //error code. added by Boern
    public static final int ERROR_CODE_LOGIN_FAILURE = 0X01;
    public static final int ERROR_CODE_SYNC_FAILURE = 0X02;
    
    //bundle key.added by Boern
    public static final String KEY_ERROR_CODE = "errorCode";
    public static final String KEY_ERROR_MSG = "errorMsg";
    public static final String KEY_SYNC_TYPE = "syncType";
    
    //同步类型：
    public static final int SYNC_TYPE_FIRST = 0X00;
    public static final int SYNC_TYPE_RESYNC = 0X01;
    public static final int SYNC_TYPE_NOSYNC = 0X02;
    
    //login type
    public static final int LOGIN_TYPE_FIRST = 0X01;
    public static final int LOGIN_TYPE_RELOGIN= 0X02;
    public static final int LOGIN_TYPE_NORMAL = 0X03;
    
    public static final int LOGIN_SUCCESS = 0X04;
    public static final int LOGIN_FAILURE = 0X05;
    
}
