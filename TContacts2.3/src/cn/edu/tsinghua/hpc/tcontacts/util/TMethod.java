package cn.edu.tsinghua.hpc.tcontacts.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.IContentService;
import android.content.res.AssetFileDescriptor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.MemoryFile;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CursorAdapter;
import android.widget.QuickContactBadge;

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
			Log.w("Method", e.toString());
		}
		return fd;
	}

	public static void setStrictProjectionMap(SQLiteQueryBuilder qb, boolean f) {
		try {
			Method methodSetStrictProjectionMap = Class.forName(
					"android.database.sqlite.SQLiteQueryBuilder").getMethod(
					"setStrictProjectionMap", new Class[] { boolean.class });
			methodSetStrictProjectionMap.setAccessible(true);
			methodSetStrictProjectionMap.invoke(qb, new Object[] { f });
		} catch (Exception e) {
			Log.w("Method", e.toString());
		}
	}

	public static String concatenateWhere(String a, String b) {
		// DatabaseUtils
		if (TextUtils.isEmpty(a)) {
			return b;
		}
		if (TextUtils.isEmpty(b)) {
			return a;
		}
		return "(" + a + ") AND (" + b + ")";
	}

	public static void setProgressNumberFormat(ProgressDialog mProgressDialog,
			String str) {
		try {
			Method setProgressNumberFormatMethod = Class.forName(
					"android.app.ProgressDialog").getMethod(
					"setProgressNumberFormat", new Class[] { String.class });
			setProgressNumberFormatMethod.setAccessible(true);
			setProgressNumberFormatMethod.invoke(mProgressDialog,
					new Object[] { str });
		} catch (Exception e) {
			Log.w("Method", e.toString());
		}
	}

	public static TelephonyManager getDefault() {
		TelephonyManager temp = null;
		try {
			Method getDefaultMethod = Class.forName(
					"android.telephony.TelephonyManager").getMethod(
					"getDefault", new Class[] {});
			getDefaultMethod.setAccessible(true);
			temp = (TelephonyManager) getDefaultMethod.invoke(null,
					new Object[] {});
		} catch (Exception e) {
			Log.w("Method", e.toString());
		}
		return temp;
	}

	
	/*
	public static boolean mDataValid(CursorAdapter adapter) {
		boolean temp = false;
		try {
			Field getDataValid = CursorAdapter.class.getField("mDataValid");
			getDataValid.setAccessible(true);
			temp = getDataValid.getBoolean(adapter);
		} catch (Exception e) {
			Log.w("Method", e.toString());
		}
		return temp;
	}
	*/

	public static int mRowIDColumn(CursorAdapter adapter) {
		int temp = -1;
		try {
			Field getDataValid = Class.forName("android.widget.CursorAdapter")
					.getField("mRowIDColumn");
			getDataValid.setAccessible(true);
			temp = getDataValid.getInt(adapter);
		} catch (Exception e) {
			Log.w("Method", e.toString());
		}
		return temp;
	}

	public static void setSelectedContactsAppTabIndex(QuickContactBadge badge,
			int index) {
		try {
			Method method = QuickContactBadge.class
					.getMethod("setSelectedContactsAppTabIndex",
							new Class[] { int.class });
			method.setAccessible(true);
			method.invoke(badge, new Object[] { index });

		} catch (Exception e) {
			Log.w("Method", e.toString());
		}
	}

	public static IContentService getContentService() {
		IContentService temp = null;
		try {
			Method getContentServiceMethod = Class.forName(
					"android.content.ContentResolver").getMethod(
					"getContentService", new Class[] {});
			getContentServiceMethod.setAccessible(true);
			temp = (IContentService) getContentServiceMethod.invoke(null,
					new Object[] {});
		} catch (Exception e) {
			Log.w("Method", e.toString());
		}
		return temp;
	}

	public static int getType(ContentProviderOperation operation) {
		int temp = 0;
		try {
			Method getContentServiceMethod = Class.forName(
					"android.content.ContentProviderOperation").getMethod(
					"getType", new Class[] {});
			getContentServiceMethod.setAccessible(true);
			temp = (Integer) getContentServiceMethod.invoke(operation,
					new Object[] {});
		} catch (Exception e) {
			Log.w("Method", e.toString());
		}
		return temp;
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

	public static boolean isUriNumber(String number) {
		// Note we allow either "@" or "%40" to indicate a URI, in case
		// the passed-in string is URI-escaped. (Neither "@" nor "%40"
		// will ever be found in a legal PSTN number.)
		return number != null
				&& (number.contains("@") || number.contains("%40"));
	}

	public static String formatNumber(String source, int defaultFormattingType) {
		// android.telephony.PhoneNumberUtils
		SpannableStringBuilder text = new SpannableStringBuilder(source);
		PhoneNumberUtils.formatNumber(text, defaultFormattingType);
		return text.toString();
	}

}
