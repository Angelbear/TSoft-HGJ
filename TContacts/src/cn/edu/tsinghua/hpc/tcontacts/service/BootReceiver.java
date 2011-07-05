package cn.edu.tsinghua.hpc.tcontacts.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	public static final String TSYNC_REQUEST = "cn.tsinghua.hpc.TSYNC_REQUEST";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("MyContact","Recive broadcast");
		Intent serviceIntent = new Intent(context, TContactSyncService.class);
		serviceIntent.setAction("com.android.contact.service.START_SYNC_SERVICE");
		context.startService(serviceIntent);
	}

}
