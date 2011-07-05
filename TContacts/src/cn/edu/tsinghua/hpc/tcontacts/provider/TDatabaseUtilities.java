package cn.edu.tsinghua.hpc.tcontacts.provider;

import android.text.TextUtils;

public class TDatabaseUtilities {
	 public static String concatenateWhere(String a, String b) {
	        if (TextUtils.isEmpty(a)) {
	            return b;
	        }
	        if (TextUtils.isEmpty(b)) {
	            return a;
	        }

	        return "(" + a + ") AND (" + b + ")";
	    }
}
