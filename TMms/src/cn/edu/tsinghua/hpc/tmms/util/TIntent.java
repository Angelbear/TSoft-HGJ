package cn.edu.tsinghua.hpc.tmms.util;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.ContactsContract.QuickContact;

public class TIntent {
//	public static final String ACTION_CALL_PRIVILEGED = "android.intent.action.CALL_PRIVILEGED";
	public static final String ACTION_CALL_PRIVILEGED = "android.intent.action.DIAL";

	public static final String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";
	public static final String ACTION_SYNC_STATE_CHANGED = "android.intent.action.SYNC_STATE_CHANGED";

/**	not ever used
	public static final String FILTER_CONTACTS = "cn.edu.tsinghua.hpc.tcontacts.action.FILTER_CONTACTS";
	public static final String LIST_DEFAULT = "cn.edu.tsinghua.hpc.tcontacts.action.LIST_DEFAULT";
	public static final String LIST_CONTACTS = "cn.edu.tsinghua.hpc.tcontacts.action.LIST_CONTACTS";
	public static final String LIST_ALL_CONTACTS = "cn.edu.tsinghua.hpc.tcontacts.action.LIST_ALL_CONTACTS";
	public static final String LIST_CONTACTS_WITH_PHONES = "cn.edu.tsinghua.hpc.tcontacts.action.LIST_CONTACTS_WITH_PHONES";
	public static final String LIST_STARRED = "cn.edu.tsinghua.hpc.tcontacts.action.LIST_STARRED";
	public static final String LIST_FREQUENT = "cn.edu.tsinghua.hpc.tcontacts.action.LIST_FREQUENT";
	public static final String LIST_STREQUENT = "cn.edu.tsinghua.hpc.tcontacts.action.LIST_STREQUENT";
	public static final String JOIN_AGGREGATE = "cn.edu.tsinghua.hpc.tcontacts.action.JOIN_AGGREGATE";
*/
	
//	public static final String ACTION_DIAL = "tsinghua.intent.action.DIAL";
	public static final String ACTION_DIAL = "android.intent.action.DIAL";
	
	public static final String ACTION_QUICK_CONTACT = "cn.edu.tsinghua.hpc.tcontacts.action.QUICK_CONTACT";
	public static final String ACTION_VIEW = "tsinghua.intent.action.VIEW";
	public static final String ACTION_CALL_BUTTON = "tsinghua.intent.action.CALL_BUTTON";
	public static final String ACTION_INSERT_OR_EDIT = "tsinghua.intent.action.INSERT_OR_EDIT";
	public static final String ACTION_PICK = "tsinghua.intent.action.PICK";
	public static final String ACTION_GET_CONTENT = "tsinghua.intent.action.GET_CONTENT";
	public static final String ACTION_SEARCH = "tsinghua.intent.action.SEARCH";
	public static final String ACTION_CREATE_SHORTCUT = "tsinghua.intent.action.CREATE_SHORTCUT";
	public static final String ACTION_EDIT = "tsinghua.intent.action.EDIT";
	public static final String ACTION_INSERT = "tsinghua.intent.action.INSERT";
	public static final String ACTION_CREATE_LIVE_FOLDER = "tsinghua.intent.action.CREATE_LIVE_FOLDER";

	public static final String ACTION_SEND = "tsinghua.intent.action.SEND";
//	public static final String ACTION_SENDTO = "tsinghua.intent.action.SENDTO";
	public static final String ACTION_SEND_MULTIPLE = "tsinghua.intent.action.SEND_MULTIPLE";
//	public static final String ACTION_CONTENT_CHANGED = "tsinghua.intent.action.CONTENT_CHANGED";

	public static String getACTION_QUICK_CONTACT(Context ctx){
		if (isTContactsInstalled(ctx))
			return TIntent.ACTION_QUICK_CONTACT;
		else
			return "com.android.contacts.action.QUICK_CONTACT";
	}
	
	public static String getACTION_INSERT_OR_EDIT(Context ctx){
		if (isTContactsInstalled(ctx))
			return TIntent.ACTION_INSERT_OR_EDIT;
		else
			return Intent.ACTION_INSERT_OR_EDIT;
	}
	
	public static String getACTION_INSERT(Context ctx){
		if (isTContactsInstalled(ctx))
			return TIntent.ACTION_INSERT;
		else
			return Intent.ACTION_INSERT;
	}
	
	public static String getACTION_EDIT(Context ctx){
		if (isTContactsInstalled(ctx))
			return TIntent.ACTION_EDIT;
		else
			return Intent.ACTION_EDIT;
	}
	
	public static String getACTION_VIEW(Context ctx){
		if (isTContactsInstalled(ctx))
			return TIntent.ACTION_VIEW;
		else
			return Intent.ACTION_VIEW;
	}

	public static boolean isTContactsInstalled = false;
	public static void checkTContacts(Context ctx){
		PackageManager packageManager = ctx.getPackageManager();
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        for (int i = 0; i < apps.size(); i++) {
                final ResolveInfo info = apps.get(i);//get the list of apps
                final ActivityInfo activityInfo = info.activityInfo;//get the ActivityInfo
                String x = activityInfo.packageName;
                if(x.equals("cn.edu.tsinghua.hpc.tcontacts")){
                	isTContactsInstalled = true;
                }
            }
        isTContactsInstalled = false;
   	}
	public static boolean isTContactsInstalled(Context ctx){
		PackageManager packageManager = ctx.getPackageManager();
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        for (int i = 0; i < apps.size(); i++) {
                final ResolveInfo info = apps.get(i);//get the list of apps
                final ActivityInfo activityInfo = info.activityInfo;//get the ActivityInfo
                String x = activityInfo.packageName;
                if(x.equals("cn.edu.tsinghua.hpc.tcontacts")){
                	isTContactsInstalled = true;
                 	return true;
               }
            }
        isTContactsInstalled = false;
        return false;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
