package cn.edu.tsinghua.hpc.tcontacts;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import cn.edu.tsinghua.hpc.tcontacts.service.BootReceiver;

public class ContactsPreferenceActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	// Menu entries
	private static final int MENU_RESTORE_DEFAULTS = 1;

	public static final String TRANSPARENT_SYNC = "pref_key_contacts_transparent_sync";
	private PendingIntent pendingIntent;
	private NotificationManager mNM;

	private static final int TSYNC_START = 0;
	private static final int TSYNC_STOP = 1;

	private CheckBoxPreference mSyncInterval;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
		addPreferencesFromResource(R.xml.preferences);
		Intent intent = new Intent(this, BootReceiver.class);
		pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mSyncInterval = (CheckBoxPreference) findPreference(TRANSPARENT_SYNC);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.clear();
		menu.add(0, MENU_RESTORE_DEFAULTS, 0, "ª÷∏¥…Ë÷√");
		return true;
	}

	private void setTSyncInterval(int interval) {
		getPreferenceScreen().getSharedPreferences().edit().putInt(
				"sync_interval", interval).commit();
		mSyncInterval.setSummary("Current sync interval is " + interval
				+ " minitues");
	}

	private int getTSyncInterval(Context ctx) {
		return 60;

	}

	

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference == mSyncInterval) {
		
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
		CharSequence text = "Contact TSync On";

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.stat_tsync_on,
				text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, ContactsPreferenceActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, "Contact TSyncService", text,
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
		CharSequence text = "Contact TSync Off";

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.stat_tsync_off,
				text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, ContactsPreferenceActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, "Contact TSyncService", text,
				contentIntent);

		// Send the notification.
		// We use a layout id because it is a unique number. We use it later to
		// cancel.

		mNM.notify(TSYNC_STOP, notification);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"pref_key_contacts_transparent_sync", true)) {
			mSyncInterval.setChecked(true);
		}else {
			Log.d("test","not true");
		}
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_RESTORE_DEFAULTS:
			restoreDefaultPreferences();
			return true;
		}
		return false;
	}

	private void restoreDefaultPreferences() {
		PreferenceManager.getDefaultSharedPreferences(this).edit().clear()
				.commit();
		setPreferenceScreen(null);
		addPreferencesFromResource(R.xml.preferences);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d("MyContact", "onSharedPreferenceChanged");
		if (key.equals(TRANSPARENT_SYNC)) {
			boolean isTsyncEnabled = sharedPreferences.getBoolean(
					TRANSPARENT_SYNC, true);
			AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			if (isTsyncEnabled) {
				alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System
						.currentTimeMillis() + (1000), 1000 * 60 * getTSyncInterval(this),
						pendingIntent);
				//showStartNotification();
			} else {
				alarmManager.cancel(pendingIntent);
				//showStopNotification();
			}
		}
	}
}
