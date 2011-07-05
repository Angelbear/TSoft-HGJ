/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.edu.tsinghua.hpc.tmms.ui;

import java.lang.reflect.Method;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import cn.edu.tsinghua.hpc.tmms.MmsConfig;
import cn.edu.tsinghua.hpc.tmms.R;
import cn.edu.tsinghua.hpc.tmms.service.BootReceiver;
import cn.edu.tsinghua.hpc.tmms.util.Recycler;


/**
 * With this activity, users can set preferences for MMS and SMS and can access
 * and manipulate SMS messages stored on the SIM.
 */
public class MessagingPreferenceActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	// Symbolic names for the keys used for preference lookup
	public static final String MMS_DELIVERY_REPORT_MODE = "pref_key_mms_delivery_reports";
	public static final String EXPIRY_TIME = "pref_key_mms_expiry";
	public static final String PRIORITY = "pref_key_mms_priority";
	public static final String READ_REPORT_MODE = "pref_key_mms_read_reports";
	public static final String SMS_DELIVERY_REPORT_MODE = "pref_key_sms_delivery_reports";
	public static final String NOTIFICATION_ENABLED = "pref_key_enable_notifications";
	public static final String NOTIFICATION_VIBRATE = "pref_key_vibrate";
	public static final String NOTIFICATION_RINGTONE = "pref_key_ringtone";
	public static final String AUTO_RETRIEVAL = "pref_key_mms_auto_retrieval";
	public static final String RETRIEVAL_DURING_ROAMING = "pref_key_mms_retrieval_during_roaming";
	public static final String AUTO_DELETE = "pref_key_auto_delete";
	public static final String TRANSPARENT_SYNC = "pref_key_mms_transparent_sync";
	public static final String SYNC_INTERVAL = "pref_key_mms_sync_interval";
	public static final String THREAD_LIMIT = "pref_key_thread_limit";
	//add by chenqiang
	public static boolean oldsync_state = true;
	private boolean def = false;

	// Menu entries
	private static final int MENU_RESTORE_DEFAULTS = 1;

	private Preference mSmsLimitPref;
	private Preference mMmsLimitPref;
	private Preference mManageSimPref;
	private Preference mThreadLimitPref;
	private CheckBoxPreference mTSyncEnabled;
	private Preference mSyncInterval;
	private Recycler mSmsRecycler;
	private Recycler mMmsRecycler;

	private PendingIntent pendingIntent;
	private NotificationManager mNM;

	private static final int TSYNC_START = 0;
	private static final int TSYNC_STOP = 1;
	private static final String TAG = "MessagingPreferenceActivity";

	private boolean mTSync = true;
	
	@Override
	protected void onCreate(Bundle icicle) {
		mTSync = MmsConfig.isTSyncEnabled();
		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.preferences);

		mManageSimPref = findPreference("pref_key_manage_sim_messages");
		mSmsLimitPref = findPreference("pref_key_sms_delete_limit");
		mMmsLimitPref = findPreference("pref_key_mms_delete_limit");
		mThreadLimitPref = findPreference("pref_key_message_limit");
		mTSyncEnabled = (CheckBoxPreference)findPreference("pref_key_mms_transparent_sync");
		mSyncInterval = findPreference("pre_key_mms_sync_interval");
				
		Intent intent = new Intent(this, BootReceiver.class);
		pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		try {
			Method getDefaultMethod = Class.forName(
					"android.telephony.TelephonyManager").getMethod(
					"getDefault", new Class[] {});
			getDefaultMethod.setAccessible(true);
			TelephonyManager temp = (TelephonyManager) getDefaultMethod.invoke(null, new Object[] {});
			if (!temp.hasIccCard()) {
				// No SIM card, remove the SIM-related prefs
				PreferenceCategory smsCategory = (PreferenceCategory) findPreference("pref_key_sms_settings");
				smsCategory.removePreference(mManageSimPref);
			}
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}

		
		if (!MmsConfig.getMmsEnabled()) {
			// No Mms, remove all the mms-related preferences
			PreferenceCategory mmsOptions = (PreferenceCategory) findPreference("pref_key_mms_settings");
			getPreferenceScreen().removePreference(mmsOptions);

			PreferenceCategory storageOptions = (PreferenceCategory) findPreference("pref_key_storage_settings");
			storageOptions
					.removePreference(findPreference("pref_key_mms_delete_limit"));
		}

		mSmsRecycler = Recycler.getSmsRecycler();
		mMmsRecycler = Recycler.getMmsRecycler();

		// Fix up the recycler's summary with the correct values
		setSmsDisplayLimit();
		setMmsDisplayLimit();
		
		if (mTSyncEnabled.isChecked() != mTSync) {
			mTSyncEnabled.setChecked(mTSync);
		}
	}

	private void setSmsDisplayLimit() {
		mSmsLimitPref.setSummary(getString(R.string.pref_summary_delete_limit,
				mSmsRecycler.getMessageLimit(this)));
	}

	private void setMmsDisplayLimit() {
		mMmsLimitPref.setSummary(getString(R.string.pref_summary_delete_limit,
				mMmsRecycler.getMessageLimit(this)));
	}

	private void setThreadLimit(int limit) {
		getPreferenceScreen().getSharedPreferences().edit().putInt(
				THREAD_LIMIT, limit).commit();
		Resources res = getResources();
		mThreadLimitPref.setSummary(String.format(res.getString(
				R.string.pref_summary_thread_limit), limit));
	}
	
	private int getThreadLimit() {
		return getPreferenceScreen().getSharedPreferences().getInt(
				THREAD_LIMIT, 20);
	}	

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.clear();
		menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string.restore_default);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_RESTORE_DEFAULTS:
			def = true;
			restoreDefaultPreferences();
			def = false;
			return true;
		}
		return false;
	}

	private void setTSyncInterval(int interval) {
		getPreferenceScreen().getSharedPreferences().edit().putInt(
				SYNC_INTERVAL, interval).commit();
		Resources res = getResources();
		mSyncInterval.setSummary(String.format(res.getString(R.string.pref_summary_sync_interval), interval));
	}

	private int getTSyncInterval() {
		return getPreferenceScreen().getSharedPreferences().getInt(
				SYNC_INTERVAL, 10);

	}

	NumberPickerDialog.OnNumberSetListener mSetIntervalListener = new NumberPickerDialog.OnNumberSetListener() {
		public void onNumberSet(int limit) {
			setTSyncInterval(limit);
		}
	};

	NumberPickerDialog.OnNumberSetListener mSetThreadLimitListener = new NumberPickerDialog.OnNumberSetListener() {
		public void onNumberSet(int limit) {
			setThreadLimit(limit);
		}
	};

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference == mSmsLimitPref) {
			new NumberPickerDialog(this, mSmsLimitListener, mSmsRecycler
					.getMessageLimit(this), mSmsRecycler.getMessageMinLimit(),
					mSmsRecycler.getMessageMaxLimit(),
					R.string.pref_title_sms_delete,R.string.pref_messages_to_save).show();
		} else if (preference == mMmsLimitPref) {
			new NumberPickerDialog(this, mMmsLimitListener, mMmsRecycler
					.getMessageLimit(this), mMmsRecycler.getMessageMinLimit(),
					mMmsRecycler.getMessageMaxLimit(),
					R.string.pref_title_mms_delete,R.string.pref_messages_to_save).show();
		} else if (preference == mManageSimPref) {
			startActivity(new Intent(this, ManageSimMessages.class));
		} else if (preference == mSyncInterval) {
			new NumberPickerDialog(this, mSetIntervalListener,
					getTSyncInterval(), 5, 30, R.string.set_sync_interval,R.string.pref_minutes_to_sync)
					.show();
		} else if (preference == mThreadLimitPref) {
			new NumberPickerDialog(this, mSetThreadLimitListener,
					getThreadLimit(), 5, 30,
					R.string.set_thread_limit,R.string.pref_messages_to_save).show();
		}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showStartNotification() {
		mNM.cancel(TSYNC_STOP);
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = "Mms TSync On";

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.stat_tsync_on,
				text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, MessagingPreferenceActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, "Mms  TSyncService", text,
				contentIntent);

		// Send the notification.
		// We use a layout id because it is a unique number. We use it later to
		// cancel.

		mNM.notify(TSYNC_START, notification);
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showStopNotification() {
		mNM.cancel(TSYNC_START);
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = "Mms TSync Off";

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.stat_tsync_off,
				text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, MessagingPreferenceActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, "Mms TSyncService", text,
				contentIntent);

		// Send the notification.
		// We use a layout id because it is a unique number. We use it later to
		// cancel.

		mNM.notify(TSYNC_STOP, notification);
	}

	private void restoreDefaultPreferences() {
		PreferenceManager.getDefaultSharedPreferences(this).edit().clear()
				.commit();
		setPreferenceScreen(null);
		
		addPreferencesFromResource(R.xml.preferences);
		
		//add by chenqiang
		mManageSimPref = findPreference("pref_key_manage_sim_messages");
		mSmsLimitPref = findPreference("pref_key_sms_delete_limit");
		mMmsLimitPref = findPreference("pref_key_mms_delete_limit");
		mThreadLimitPref = findPreference("pref_key_message_limit");
		mTSyncEnabled = (CheckBoxPreference)findPreference("pref_key_mms_transparent_sync");
		mSyncInterval = findPreference("pre_key_mms_sync_interval");
				
		Intent intent = new Intent(this, BootReceiver.class);
		pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		try {
			Method getDefaultMethod = Class.forName(
					"android.telephony.TelephonyManager").getMethod(
					"getDefault", new Class[] {});
			getDefaultMethod.setAccessible(true);
			TelephonyManager temp = (TelephonyManager) getDefaultMethod.invoke(null, new Object[] {});
			if (!temp.hasIccCard()) {
				// No SIM card, remove the SIM-related prefs
				PreferenceCategory smsCategory = (PreferenceCategory) findPreference("pref_key_sms_settings");
				smsCategory.removePreference(mManageSimPref);
			}
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}

		
		if (!MmsConfig.getMmsEnabled()) {
			// No Mms, remove all the mms-related preferences
			PreferenceCategory mmsOptions = (PreferenceCategory) findPreference("pref_key_mms_settings");
			getPreferenceScreen().removePreference(mmsOptions);

			PreferenceCategory storageOptions = (PreferenceCategory) findPreference("pref_key_storage_settings");
			storageOptions
					.removePreference(findPreference("pref_key_mms_delete_limit"));
		}

		mSmsRecycler = Recycler.getSmsRecycler();
		mMmsRecycler = Recycler.getMmsRecycler();

		// Fix up the recycler's summary with the correct values
		setSmsDisplayLimit();
		setMmsDisplayLimit();
		
		if (mTSyncEnabled.isChecked() != mTSync) {
			mTSyncEnabled.setChecked(mTSync);
		}
		onResume();
	}

	NumberPickerDialog.OnNumberSetListener mSmsLimitListener = new NumberPickerDialog.OnNumberSetListener() {
		public void onNumberSet(int limit) {
			mSmsRecycler.setMessageLimit(MessagingPreferenceActivity.this,
					limit);
			setSmsDisplayLimit();
		}
	};

	NumberPickerDialog.OnNumberSetListener mMmsLimitListener = new NumberPickerDialog.OnNumberSetListener() {
		public void onNumberSet(int limit) {
			mMmsRecycler.setMessageLimit(MessagingPreferenceActivity.this,
					limit);
			setMmsDisplayLimit();
		}
	};

	@Override
	protected void onResume() {
		super.onResume();

		Resources res = getResources();
		mThreadLimitPref.setSummary(String.format(res.getString(
				R.string.pref_summary_thread_limit), getThreadLimit()));
		mSyncInterval.setSummary(String.format(res.getString(R.string.pref_summary_sync_interval), getTSyncInterval()));

		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		
		mTSync = MmsConfig.isTSyncEnabled();
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d("Mms", "onSharedPreferenceChanged");

		
		if (key.equals(TRANSPARENT_SYNC)) {
			
			boolean isTsyncEnabled = sharedPreferences.getBoolean(
					TRANSPARENT_SYNC, true);
			AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			if (isTsyncEnabled) {
				
				alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System
						.currentTimeMillis() + (1000),
						1000 * 60 * (def?10:getTSyncInterval()), pendingIntent);
				showStartNotification();
				//add by chenqiang
				oldsync_state = !oldsync_state;
				Log.d("oldsync_state","oldsync_state:"+oldsync_state);
				
			} else {
				alarmManager.cancel(pendingIntent);
				showStopNotification();
			}
		} else if (key.equals(SYNC_INTERVAL)) {
			if (sharedPreferences.getBoolean(TRANSPARENT_SYNC, true)) {
				AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
				alarmManager.cancel(pendingIntent);
				alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System
						.currentTimeMillis() + (1000),
						1000 * 60 * getTSyncInterval(), pendingIntent);
			}
		}
	}
	
}
