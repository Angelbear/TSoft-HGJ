package cn.edu.tsinghua.hpc.tcontacts;

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Contacts;
import android.util.Log;

public class ContactsApp extends Application {
	

	@Override
	public void onCreate() {
		super.onCreate();
		
	}

	@Override
	public void onTerminate(){
		
		super.onTerminate();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	
}
