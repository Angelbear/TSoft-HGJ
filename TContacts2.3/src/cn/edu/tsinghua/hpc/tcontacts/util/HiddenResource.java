package cn.edu.tsinghua.hpc.tcontacts.util;

import java.lang.reflect.Field;

import android.util.Log;

public class HiddenResource {
	public static final String TAG = "HiddenResource";
	
	public static final int getArray(String res) {
		try {
			Field f = Class.forName("android.R$array").getField(
					res);
			return f.getInt(null);
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}
		return 0;
	}
	
	public static final int getString(String res) {
		try {
			Field f = Class.forName("android.R$string").getField(
					res);
			return f.getInt(null);
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}
		return 0;
	}
	
	
	public static final int getDrawable(String res) {
		try {
			Field f = Class.forName("android.R$drawable").getField(
					res);
			return f.getInt(null);
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}
		return 0;
	}
	
	public static final int getStylable(String res) {
		try {
			Field f = Class.forName("android.R$styleable").getField(
					res);
			return f.getInt(null);
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}
		return 0;
	}
	
	public static final int[] getStylableArray(String res) {
		try {
			Field f = Class.forName("android.R$styleable").getField(
					res);
			return (int[]) f.get(null);
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}
		return null;
	}
}
