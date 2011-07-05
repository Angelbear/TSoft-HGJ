package cn.edu.tsinghua.hpc.tcontacts.service;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.ClientProtocolException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.NetworkInfo.State;
import android.os.IBinder;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import cn.edu.tsinghua.hpc.syncbroker.ElementNotFound;
import cn.edu.tsinghua.hpc.tcontacts.Preferences;
import cn.edu.tsinghua.hpc.tcontacts.syncaction.ContactsUtility;
import cn.edu.tsinghua.hpc.tcontacts.syncaction.SyncState;
import cn.edu.tsinghua.hpc.tcontacts.syncaction.TaskExecuter;
import cn.edu.tsinghua.hpc.tcontacts.util.TIntent;
import cn.edu.tsinghua.hpc.tcontacts.util.TContactsContract.TContacts;

public class TContactSyncService extends Service {

	private static final String TAG = "TContactSyncService";
	private ConnectionListener connectionListener;

	private Timer syncTimer;
	private SyncTask syncTask;
	private static final long SYNC_DELAY = 3000;
	public static final Uri CONTENT_QUERY_URI = Uri.withAppendedPath(
			TContacts.CONTENT_URI, "query");

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "Service create");
		connectionListener = new ConnectionListener(this
				.getApplicationContext());

		TelephonyManager mTelephonyMgr = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
		mTelephonyMgr.listen(connectionListener,
				PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

		syncTimer = new Timer();
		syncTask = new SyncTask();

	}

	@Override
	public void onStart(Intent intent, int startId) {
		syncTask.cancel();
		syncTimer.purge();
		if (isNetworkConnected()) {
			syncTask = new SyncTask();
			syncTimer.schedule(syncTask, SYNC_DELAY);
		}
	}

	@Override
	public void onDestroy() {
	}

	private boolean isNetworkConnected() {
		ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (conMan.getActiveNetworkInfo() != null
				&& conMan.getActiveNetworkInfo().getState() == State.CONNECTED) {
			return true;
		}
		return false;
	}

	private class SyncTask extends TimerTask {

		@Override
		public void run() {

			try {

				if (Preferences.isTSyncEnabled(TContactSyncService.this)) {
					// if (TContactSyncHelper.tryLock(TContactSyncService.this))
					// {
					Intent broadcastIntent = new Intent();
					broadcastIntent
							.setAction(TIntent.ACTION_SYNC_STATE_CHANGED);
					broadcastIntent.putExtra("active", true);
					sendBroadcast(broadcastIntent);

					// android.os.Debug.waitForDebugger();

					Log.d(TAG, "addContacts");
					addContacts();
					Log.d(TAG, "updateContacts");
					updateContacts();
					Log.d(TAG, "recoverContacts");
					recoverContacts();
					Log.d(TAG, "deleteContacts");
					deleteContacts();
					Log.d(TAG, "removeContacts");
					removeContacts();

					broadcastIntent.putExtra("active", false);
					sendBroadcast(broadcastIntent);
					// }

				} else {
					executeOfflineSync();
				}
			} catch (Exception e) {
				Log.d("TContact", e.getMessage());
			} finally {
				TContactSyncHelper.releaseLock(TContactSyncService.this);
			}
		}

		private void executeOfflineSync() {
			/**
			 * TODO add offline sync as the ContactsProvider does now it just do
			 * nothing and wait for TSyncEnable
			 */

		}

		private void addContacts() {
			Context ctx = TContactSyncService.this;
			// add by chenqiang
			// Uri CONTENT_QUERY_URI = TRawContacts.CONTENT_URI;
			Cursor c = ctx.getContentResolver().query(CONTENT_QUERY_URI,
					new String[] { Contacts._ID, "guid", "sync_state" },
					"guid IS NULL OR guid = -1", null, null);
			if (c != null && c.moveToFirst()) {
				// Log.d(TAG, "-----------"+c.getCount());
				do {
					String id = c.getString(c.getColumnIndex(Contacts._ID));
					Uri resultUri = Uri.withAppendedPath(TContacts.CONTENT_URI,
							id);
					// // //add by chenqiang
					// Uri resultUri = Uri.withAppendedPath(
					// TRawContacts.CONTENT_URI, id);
					try {
						// int guid = TaskExecuter.executeAddTask(id, ctx);
						// Uri resultUri = Uri.withAppendedPath(
						// TContacts.CONTENT_URI, id);
						// ContactsUtility.setGuid(ctx, resultUri, guid);

						ContactsUtility.setGuid(ctx, resultUri, -999);
						int guid = TaskExecuter.executeAddTask(id, ctx);
						ContactsUtility.setGuid(ctx, resultUri, guid);

						ContactsUtility.markContact(ctx, resultUri,
								SyncState.SYNC_STATE_PRESENT);

					} catch (ClientProtocolException e) {
						Log.d(TAG, "ClientProtocolException");
						ContactsUtility.setGuid(ctx, resultUri, -1);
					} catch (ElementNotFound e) {
						Log.d(TAG, "ElementNotFound");
						ContactsUtility.setGuid(ctx, resultUri, -1);
					} catch (IOException e) {
						Log.d(TAG, "IOException");
						ContactsUtility.setGuid(ctx, resultUri, -1);
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void updateContacts() {
			Context ctx = TContactSyncService.this;
			Cursor c = ctx.getContentResolver().query(CONTENT_QUERY_URI,
					new String[] { Contacts._ID, "guid", "sync_state" },
					"sync_state = '" + SyncState.SYNC_STATE_UPDATED + "'",
					null, null);
			if (c != null && c.moveToFirst()) {
				do {
					String id = c.getString(c.getColumnIndex(Contacts._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					try {
						TaskExecuter.executeUpdateTask(id, guid, ctx);
						Uri resultUri = Uri.withAppendedPath(
								TContacts.CONTENT_URI, id);

						ContactsUtility.markContact(ctx, resultUri,
								SyncState.SYNC_STATE_PRESENT);

					} catch (ClientProtocolException e) {
						Log.d(TAG, "ClientProtocolException");
					} catch (ElementNotFound e) {
						Log.d(TAG, "ElementNotFound");
					} catch (IOException e) {
						Log.d(TAG, "IOException");
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void recoverContacts() {
			Context ctx = TContactSyncService.this;
			Cursor c = ctx.getContentResolver().query(CONTENT_QUERY_URI,
					new String[] { Contacts._ID, "guid", "sync_state" },
					"sync_state = '" + SyncState.SYNC_STATE_RECOVER + "'",
					null, null);
			if (c != null && c.moveToFirst()) {
				do {
					String id = c.getString(c.getColumnIndex(Contacts._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					try {
						// if (TaskExecuter.executeUpdateTask(id, guid, ctx)) {
						if (TaskExecuter.executeRecoverTask(guid, ctx)) {
							Uri resultUri = Uri.withAppendedPath(
									TContacts.CONTENT_URI, id);
							ContactsUtility.markContact(ctx, resultUri,
									SyncState.SYNC_STATE_PRESENT);
						}
						// }
					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void deleteContacts() {
			Context ctx = TContactSyncService.this;
			Cursor c = ctx.getContentResolver().query(CONTENT_QUERY_URI,
					new String[] { Contacts._ID, "guid", "sync_state" },
					"sync_state = '" + SyncState.SYNC_STATE_DELETE + "'", null,
					null);
			if (c != null && c.moveToFirst()) {
				do {
					String id = c.getString(c.getColumnIndex(Contacts._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					try {
						// if (TaskExecuter.executeUpdateTask(id, guid, ctx)) {
						if (TaskExecuter.executeDeleteTask(guid, ctx)) {
							Uri resultUri = Uri.withAppendedPath(
									TContacts.CONTENT_URI, id);
							ContactsUtility.markContact(ctx, resultUri,
									SyncState.SYNC_STATE_DELETED);
						}
						// }
					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void removeContacts() {
			Context ctx = TContactSyncService.this;
			// add by chenqiang
			// Uri CONTENT_QUERY_URI = TRawContacts.CONTENT_URI;
			Cursor c = ctx.getContentResolver().query(CONTENT_QUERY_URI,
					new String[] { Contacts._ID, "guid", "sync_state" },
					"sync_state = '" + SyncState.SYNC_STATE_REMOVE + "'", null,
					null);
			if (c != null && c.moveToFirst()) {
				do {
					String id = c.getString(c.getColumnIndex(Contacts._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					try {
						// if (TaskExecuter.executeUpdateTask(id, guid, ctx)) {
						if (TaskExecuter.executeFinalDelete(guid, ctx)) {
							Uri resultUri = Uri.withAppendedPath(
									TContacts.CONTENT_URI, id);
							ctx.getContentResolver().delete(resultUri, null,
									null);
							// //add by chenqiang
							// // int row =
							// ctx.getContentResolver().delete(TRawContacts.CONTENT_URI,
							// //
							// "sync_state='"+SyncState.SYNC_STATE_REMOVE+"'",
							// null);
							// // Log.v(TAG, "*-----------********"+row);
							// //add by chenqiang
							// SQLiteOpenHelper mOpenHelper =
							// ContactsDatabaseHelper.getInstance(ctx);
							// SQLiteDatabase db =
							// mOpenHelper.getWritableDatabase();
							// db.delete("raw_contacts","sync_state='"+SyncState.SYNC_STATE_REMOVE+"'",
							// null);
						}
						// }
					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (c.moveToNext());
			}
			c.close();
		}

	}
}
