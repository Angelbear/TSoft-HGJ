package cn.edu.tsinghua.hpc.tmms.util;

import java.lang.reflect.Method;
import java.util.ArrayList;

import com.android.internal.telephony.CallerInfo;

import android.R.bool;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.MemoryFile;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

public class TMethod {
	public static AssetFileDescriptor memoryFile(MemoryFile memoryFile) {
		AssetFileDescriptor fd = null;
		try {
			Method methodDeactivate = Class.forName("android.os.MemoryFile")
					.getDeclaredMethod("deactivate", new Class[] {});
			methodDeactivate.setAccessible(true);
			methodDeactivate.invoke(memoryFile, new Object[] {});
			// memoryFile.deactivate();
			Method methodFromMemoryFile = Class.forName(
					"android.content.res.AssetFileDescriptor").getMethod(
					"fromMemoryFile", new Class[] { MemoryFile.class });
			methodFromMemoryFile.setAccessible(true);
			fd = (AssetFileDescriptor) methodFromMemoryFile.invoke(null,
					new Object[] { memoryFile });
			// fd = AssetFileDescriptor.fromMemoryFile(memoryFile);
		} catch (Exception e) {
			Log.w("Method",e.toString());
		}
		return fd;
	}
	
	public static void setStrictProjectionMap(SQLiteQueryBuilder qb, boolean f){
		try {
			Method methodSetStrictProjectionMap = Class.forName("android.database.sqlite.SQLiteQueryBuilder")
					.getMethod("setStrictProjectionMap", new Class[] {boolean.class});
			methodSetStrictProjectionMap.setAccessible(true);
			methodSetStrictProjectionMap.invoke(qb, new Object[]{f});
		} catch (Exception e) {
			Log.w("Method",e.toString());
		}
	}
	
	public static String concatenateWhere(String a, String b) {
		//DatabaseUtils
        if (TextUtils.isEmpty(a)) {
            return b;
        }
        if (TextUtils.isEmpty(b)) {
            return a;
        }
        return "(" + a + ") AND (" + b + ")";
    }
	
	public static void setProgressNumberFormat(ProgressDialog mProgressDialog,String str){
		try {
			Method setProgressNumberFormatMethod = Class.forName("android.app.ProgressDialog")
					.getMethod("setProgressNumberFormat", new Class[] {String.class});
			setProgressNumberFormatMethod.setAccessible(true);
			setProgressNumberFormatMethod.invoke(mProgressDialog, new Object[]{str});
		} catch (Exception e) {
			Log.w("Method",e.toString());
		}
	}
	
	public static TelephonyManager getDefault(){
		TelephonyManager temp = null;
		try {
			Method getDefaultMethod = Class.forName("android.telephony.TelephonyManager")
					.getMethod("getDefault", new Class[] {});
			getDefaultMethod.setAccessible(true);
			temp = (TelephonyManager)getDefaultMethod.invoke(null, new Object[]{});
		} catch (Exception e) {
			Log.w("Method",e.toString());
		}
		return temp;
	}
	
	public static Uri addCall(CallerInfo ci, Context context, String number,
            int presentation, int callType, long start, int duration){
		try {
			Method addCallMethod = Class.forName("android.provider.CallLog.Calls")
					.getMethod("addCall", new Class[] {CallerInfo.class, Context.class, String.class,
				            int.class, int.class, long.class, int.class});
			addCallMethod.setAccessible(true);
			return (Uri)addCallMethod.invoke(null, new Object[]{ci,context,number,presentation,callType,start,duration});
		} catch (Exception e) {
			Log.w("Method",e.toString());
		}
		return null;
	}
	
	public static CompatibilityInfo getCompatibilityInfo(Resources re){
		try {
			Method getCompatibilityInfoMethod = Class.forName("android.content.res.Resources")
					.getMethod("getCompatibilityInfo", new Class[] {});
			getCompatibilityInfoMethod.setAccessible(true);
			return (CompatibilityInfo)getCompatibilityInfoMethod.invoke(re, new Object[]{});
		} catch (Exception e) {
			Log.w("Method",e.toString());
		}
		return null;
	}
	
	public static Boolean deleteMessageFromIcc(int s) {
		try {
			Method deleteMessageFromIccMethod = Class.forName(
					"android.telephony.SmsManager").getMethod(
					"deleteMessageFromIcc", new Class[] { int.class });
			deleteMessageFromIccMethod.setAccessible(true);
			return (Boolean) deleteMessageFromIccMethod.invoke("smsManager",
					new Object[] { s });
		} catch (Exception e) {
			Log.w("Method", e.toString());
		}
		return null;
	}
	
	public static ArrayList<SmsMessage> getAllMessagesFromIcc(SmsManager smsManager) {
		try {
			Method getAllMessagesFromIccMethod = Class.forName(
					"android.telephony.SmsManager").getMethod(
					"getAllMessagesFromIcc", new Class[] {});
			getAllMessagesFromIccMethod.setAccessible(true);
			return (ArrayList<SmsMessage>) getAllMessagesFromIccMethod
					.invoke(smsManager, new Object[] {});
		} catch (Exception e) {
			Log.w("Method", e.toString());
		}
		return null;
	}
	
	public static ServiceState newFromBundle(Bundle bl){
		try {
			Method newFromBundleMethod = Class.forName(
					"android.telephony.ServiceState").getMethod(
					"newFromBundle", new Class[] { Bundle.class });
			newFromBundleMethod.setAccessible(true);
			return (ServiceState) newFromBundleMethod
					.invoke(null, new Object[] { bl });
		} catch (Exception e) {
			Log.w("Method", e.toString());
		}
		return null;
	}

	public static Bitmap getFrameAt(MediaPlayer  mp, int num){
		try {
			Method getFrameAtMethod = Class.forName("android.media.MediaPlayer").getMethod("getFrameAt", new Class[]{int.class}); 
            getFrameAtMethod.setAccessible(true);
            return (Bitmap) getFrameAtMethod.invoke(mp, new Object[]{num});
		} catch (Exception e) {
			Log.w("Method", e.toString());
		}
		return null;
	}
	
}
