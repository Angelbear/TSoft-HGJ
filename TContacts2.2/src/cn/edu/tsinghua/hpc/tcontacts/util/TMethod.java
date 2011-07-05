package cn.edu.tsinghua.hpc.tcontacts.util;

import com.android.internal.telephony.CallerInfo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.MemoryFile;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;

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
    private static boolean isPrintableAscii(final char c) {
        final int asciiFirst = 0x20;
        final int asciiLast = 0x7E; 
        return (asciiFirst <= c && c <= asciiLast) || c == '\r' || c == '\n';
    }

    public static boolean isPrintableAsciiOnly(final CharSequence str) {
    	// android.text.TextUtils
        final int len = str.length();
        for (int i = 0; i < len; i++) {
            if (!isPrintableAscii(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    public static String formatNumber(String source, int defaultFormattingType) {
    	//android.telephony.PhoneNumberUtils
        SpannableStringBuilder text = new SpannableStringBuilder(source);
        PhoneNumberUtils.formatNumber(text, defaultFormattingType);
        return text.toString();
    }


}
