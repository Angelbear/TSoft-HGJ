package cn.edu.tsinghua.hpc.tcontacts;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Preferences {
	private static final String TAG = "Preferences";
	private static final String FIRST_SYNC = "firstSync";
	private static boolean firstSync = true;
	
	private static final String IMSI = "imis";
	private static String thisIMSI = "";

	public static int maxStorage = 100;

	private static final String TOTAL_NUMBER = "totalNumber";
	public static int totalNumber = 0;
	
	//added by zhangbing@inspurworld.com
	private static boolean login = false;
	private static final String LOGIN="login";
	private static final String UID ="uid";
	private static final String TOKEN = "token";

	public static boolean isTSyncEnabled(Context mContext) {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		return sp.getBoolean(ContactsPreferenceActivity.TRANSPARENT_SYNC, true);
	}
	
	private static String priorIMSI = null;
	public static boolean isUserIDReserve(Context ctx,String newUid){
//		SharedPreferences settings = ctx.getSharedPreferences(
//				"cn.edu.tsinghua.hpc.tcontacts", 0);
//		priorIMSI = settings.getString(IMSI, null);
//		Log.d(TAG,"priorIMSI:"+priorIMSI);
//		TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
//		thisIMSI = tm.getSubscriberId();
//		Log.d(TAG,"thisIMSI:"+thisIMSI);
//		
//		if(priorIMSI.equals(thisIMSI) || (priorIMSI.equals("noSIM") && thisIMSI==null))	return true;
//		else if(thisIMSI == null)	thisIMSI = "noSIM";
//		
//		SharedPreferences.Editor editor = settings.edit();
//		editor.putString(IMSI, thisIMSI);
//		editor.commit();
		String uid = getUid(ctx);
		if (null!=uid || "".equals(uid)){
			return uid.equals(newUid);
		}
		return false;
	}
	
	public static void setIMSI(Context ctx){
		Log.d(TAG,"setUserID");
		TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		thisIMSI = tm.getSubscriberId();
		if(thisIMSI == null)	thisIMSI = "noSIM";	
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tcontacts", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(IMSI, thisIMSI);
		editor.commit();
	}

	public static String getIMSI(Context ctx) {
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tcontacts", 0);
		return  settings.getString(IMSI, null);
	}
	
	public static void setTotalNumber(int num, Context ctx) {
		Preferences.totalNumber = num;
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tcontacts", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(TOTAL_NUMBER, totalNumber);
		editor.commit();
	}

	public static int getTotalNumber(Context ctx) {
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tcontacts", 0);
		Preferences.totalNumber = settings.getInt(TOTAL_NUMBER, 0);
		return Preferences.totalNumber;
	}

	public static void setFirstSync(boolean firstSync, Context ctx) {
		Preferences.firstSync = firstSync;
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tcontacts", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(FIRST_SYNC, firstSync);
		editor.commit();
		if(!firstSync)	setIMSI(ctx);
	}

	public static boolean isFirstSync(Context ctx) {
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tcontacts", 0);
		Preferences.firstSync = settings.getBoolean(FIRST_SYNC, true);
		return Preferences.firstSync;
	}

	public static void wipedata(Context ctx){
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tcontacts", 0);
		SharedPreferences.Editor editor = settings.edit();
		firstSync = true;
		login = false;
		editor.putBoolean(FIRST_SYNC, firstSync);
		editor.putBoolean(LOGIN, login);
		thisIMSI = "noSIM";
		editor.putString(IMSI, thisIMSI);
		editor.commit();
	}
	/**
	 * @author zhangbing@inspurworld.com
	 * @param firstSync
	 * @param ctx
	 */
	public static void setNotLogin(boolean login, Context ctx) {
		Preferences.login = login;
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tcontacts", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(LOGIN, login);
		editor.commit();
	}
	/**
	 * @author zhangbing@inspurworld.com
	 * @param ctx
	 * @return login flag
	 */
	public static boolean isNotLogin(Context ctx) {
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tcontacts", 0);
		Preferences.login = settings.getBoolean(LOGIN, true);
		return Preferences.login;
	}
	
	/**
	 * @author zhangbing@inspurworld.com
	 * @param uid
	 * @param ctx
	 */
	public static void setUid(String uid,Context ctx){
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tcontacts", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(UID, uid);
		editor.commit();
	}
	
	public static String getUid(Context ctx){
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tcontacts", 0);
		return settings.getString(UID, "");
	}
	
	public static void setToken(String token,Context ctx){
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tcontacts", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(TOKEN, token);
		editor.commit();
	}
	
	public static String getToken(Context ctx){
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tcontacts", 0);
		return settings.getString(TOKEN, "");
	}	
}
