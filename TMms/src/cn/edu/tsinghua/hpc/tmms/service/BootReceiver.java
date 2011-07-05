package cn.edu.tsinghua.hpc.tmms.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	public static final String TSYNC_REQUEST = "cn.tsinghua.hpc.TSYNC_REQUEST";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("Mms","Recive broadcast");
		Intent serviceIntent = new Intent(context, cn.edu.tsinghua.hpc.tmms.service.TMmsSyncService.class);
		serviceIntent.setAction("cn.edu.tsinghua.hpc.tmms.service.START_SYNC_SERVICE");
		context.startService(serviceIntent);
	}

}
