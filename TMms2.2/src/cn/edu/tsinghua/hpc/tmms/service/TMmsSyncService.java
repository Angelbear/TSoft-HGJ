package cn.edu.tsinghua.hpc.tmms.service;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.ClientProtocolException;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import cn.edu.tsinghua.hpc.google.tmms.InvalidHeaderValueException;
import cn.edu.tsinghua.hpc.syncbroker.ElementNotFound;
import cn.edu.tsinghua.hpc.tmms.MmsConfig;
import cn.edu.tsinghua.hpc.tmms.provider.MmsSmsDatabaseHelper;
import cn.edu.tsinghua.hpc.tmms.syncaction.MmsUtils;
import cn.edu.tsinghua.hpc.tmms.syncaction.SyncAction;
import cn.edu.tsinghua.hpc.tmms.syncaction.SyncState;
import cn.edu.tsinghua.hpc.tmms.syncaction.TaskExecuter;
import cn.edu.tsinghua.hpc.tmms.ui.MessageUtils;
import cn.edu.tsinghua.hpc.tmms.ui.RecoverThreadActivity;
import cn.edu.tsinghua.hpc.tmms.util.TIntent;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMmsSms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TSms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TThreads;


public class TMmsSyncService extends Service {
	private ConnectionListener connectionListener;

	private Timer syncTimer;
	private SyncTask syncTask;
	private static final long SYNC_DELAY = 3000;
//	private static final long SYNC_DELAY = 1000;
	public static final String TAG = "TMmsService";
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
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
		syncTask = new SyncTask();
		syncTimer.schedule(syncTask, SYNC_DELAY);
	}

	@Override
	public void onDestroy() {
		
	}



	private class SyncTask extends TimerTask {

		@Override
		public void run() {
			synchronized (this) {
				if (MmsConfig.isTSyncEnabled()) {
					// try to lock the db for other service
					if (TMessageSyncHelper.tryLock(TMmsSyncService.this)) {
						try {
							Intent broadcastIntent = new Intent();
							broadcastIntent
									.setAction(TIntent.ACTION_SYNC_STATE_CHANGED);
							broadcastIntent.putExtra("active", true);
							sendBroadcast(broadcastIntent);
							
							executeDeSyncThreadTasks();
							executeSyncThreadTasks();

							executeDeSyncSMSTasks();
							executeDeSyncMMSTasks();
							executeSyncSMSTasks();
							executeSyncMMSTasks();

							executeAutoArchiveSMSTasks();
							executeAutoArchiveMmsTasks();

							executeAddSMSTasks();
							executeAddMmsTasks();

							executeUpdateSMSTasks();
							executeUpdateMMSTasks();

							executeDeleteThreadTasks();
							executeRemoveThreadTasks();

							executeRecoverSMSTasks();
							executeRecoverMMSTasks();

							executeDeleteSMSTasks();
							executeDeleteMMSTasks();

							executeRemoveSMSTasks();
							executeRemoveMMSTasks();
							
							broadcastIntent.putExtra("active", false);
							sendBroadcast(broadcastIntent);
						} catch (InvalidHeaderValueException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally {
							// FIXME: if release failed, then ??
							TMessageSyncHelper
									.releaseLock(TMmsSyncService.this);
						}
					}
				} else {
//					executeFakedTasks();
				}
			}
		}
		
		private void executeDeSyncThreadTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver().query(
					Uri.withAppendedPath(TMmsSms.AUTHORITY_URI, "all-threads"),//Uri.parse("content://mms-sms/all-threads/"),
					new String[] { Threads._ID, Threads.RECIPIENT_IDS,
							"sync_state", "sync_enable" },
					"sync_enable = " + SyncState.SYNC_DISABLE, null, null);
			if (c != null && c.moveToFirst()) {
				do {
					long localId = c.getLong(c.getColumnIndex(Threads._ID));
					String address = MessageUtils.getAddressByThreadId(ctx,
							localId);
					Uri uri = Uri.withAppendedPath(Uri.withAppendedPath(TMmsSms.AUTHORITY_URI, "threads")//Uri.parse("content://mms-sms/threads/")
							, String
							.valueOf(localId));
					if (SyncAction.finalDeleteThread(ctx, address)) {
						// mark the thread sync disabled
						MmsUtils.enableMessageOrThreadSyncInternal(ctx, uri,
								false);

						Uri threadUri = Uri.withAppendedPath(
								TThreads.CONTENT_URI, String.valueOf(localId));
						MmsUtils.enableMessageOrThreadSyncInternal(ctx,
								threadUri, false);

						// mark the thread
						MmsUtils.markMessageOrThread(ctx, uri,
								SyncState.SYNC_STATE_NOT_SYNC);
						// mark all the messages of the thread
						MmsUtils.markMessageOrThread(ctx, Uri.withAppendedPath(
								TThreads.CONTENT_URI, String.valueOf(localId)),
								SyncState.SYNC_STATE_NOT_SYNC);

						// Set the guids to -1
						MmsUtils.setGuid(ctx, Uri.withAppendedPath(
								TThreads.CONTENT_URI, String.valueOf(localId)),
								-1);
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeSyncThreadTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver().query(
					Uri.withAppendedPath(TMmsSms.AUTHORITY_URI, "all-threads"),
					//Uri.parse("content://mms-sms/all-threads/"),
					new String[] { Threads._ID, Threads.RECIPIENT_IDS,
							"sync_state", "sync_enable" },
					"sync_enable = " + SyncState.SYNC_ENABLE, null, null);
			if (c != null && c.moveToFirst()) {
				do {
					long localId = c.getLong(c.getColumnIndex(Threads._ID));
					Uri uri = Uri.withAppendedPath(Uri.withAppendedPath(TMmsSms.AUTHORITY_URI, "threads"),
							//Uri.parse("content://mms-sms/threads/"), 
							String.valueOf(localId));
					// mark the thread sync enabled
					MmsUtils.enableMessageOrThreadSyncInternal(ctx, uri, true);
					Uri threadUri = Uri.withAppendedPath(TThreads.CONTENT_URI,
							String.valueOf(localId));
					MmsUtils.enableMessageOrThreadSyncInternal(ctx, threadUri,
							true);
				} while (c.moveToNext());
			}
            c.close();
		}

		private void executeAutoArchiveSMSTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor mSmsCursor = ctx.getContentResolver()
					.query(
							TSms.CONTENT_URI,
							new String[] { Sms._ID, "guid", "sync_state",
									"sync_enable" },
							"sync_enable = 1 AND guid IS NOT NULL AND guid <> -1 AND sync_state = '"
									+ SyncState.SYNC_STATE_ARCHIVED + "'",
							null, Sms.DATE + " ASC");
			if (mSmsCursor != null && mSmsCursor.moveToFirst()) {
				do {
					String guid = mSmsCursor.getString(mSmsCursor
							.getColumnIndex("guid"));
					try {
						if (TaskExecuter.executeAchieveTask(guid, ctx)) {
							long localId = mSmsCursor.getLong(mSmsCursor
									.getColumnIndex(Sms._ID));
							Uri uri = Uri.withAppendedPath(TSms.CONTENT_URI,
									String.valueOf(localId));
							ctx.getContentResolver().delete(uri, null, null);
						}
					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (mSmsCursor.moveToNext());
				mSmsCursor.close();
			}
			mSmsCursor.close();
		}

		private void executeAutoArchiveMmsTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor mMmsCursor = ctx.getContentResolver()
					.query(
							TMms.CONTENT_URI,
							new String[] { Mms._ID, "guid", "sync_state",
									"sync_enable" },
							"sync_enable = 1 AND guid IS NOT NULL AND guid <> -1 AND sync_state = '"
									+ SyncState.SYNC_STATE_ARCHIVED + "'",
							null, Mms.DATE + " ASC");
			if (mMmsCursor != null && mMmsCursor.moveToFirst()) {
				do {
					String guid = mMmsCursor.getString(mMmsCursor
							.getColumnIndex("guid"));
					try {
						if (TaskExecuter.executeAchieveTask(guid, ctx)) {
							long localId = mMmsCursor.getLong(mMmsCursor
									.getColumnIndex(Mms._ID));
							Uri uri = Uri.withAppendedPath(TMms.CONTENT_URI,
									String.valueOf(localId));
							ctx.getContentResolver().delete(uri, null, null);
						}
					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (mMmsCursor.moveToNext());
				mMmsCursor.close();
			}
            mMmsCursor.close();
		}

		private void executeDeSyncSMSTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver()
					.query(
							TSms.CONTENT_URI,
							new String[] { Sms._ID, "guid", "sync_state",
									"sync_enable" },
							"sync_enable = " + SyncState.SYNC_DISABLE, null,
							null);
			if (c != null && c.moveToFirst()) {
				do {
					long localId = c.getLong(c.getColumnIndex(Sms._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					Uri uri = Uri.withAppendedPath(TSms.CONTENT_URI, String
							.valueOf(localId));
					try {
						if (TaskExecuter.executeFinalDelete(guid, ctx)) {
							MmsUtils.enableMessageOrThreadSyncInternal(ctx,
									uri, false);
							MmsUtils.markMessageOrThread(ctx, uri,
									SyncState.SYNC_STATE_NOT_SYNC);
							MmsUtils.setGuid(ctx, uri, -1);
						}
					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeDeSyncMMSTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver()
					.query(
							TMms.CONTENT_URI,
							new String[] { Mms._ID, "guid", "sync_state",
									"sync_enable" },
							"sync_enable = " + SyncState.SYNC_DISABLE, null,
							null);
			if (c != null && c.moveToFirst()) {
				do {
					long localId = c.getLong(c.getColumnIndex(Mms._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					Uri uri = Uri.withAppendedPath(TMms.CONTENT_URI, String
							.valueOf(localId));
					try {
						if (TaskExecuter.executeFinalDelete(guid, ctx)) {
							MmsUtils.enableMessageOrThreadSyncInternal(ctx,
									uri, false);
							MmsUtils.markMessageOrThread(ctx, uri,
									SyncState.SYNC_STATE_NOT_SYNC);
							MmsUtils.setGuid(ctx, uri, -1);
						}
					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeSyncSMSTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver()
					.query(
							TSms.CONTENT_URI,
							new String[] { Sms._ID, "guid", "sync_state",
									"sync_enable" },
							"sync_enable = " + SyncState.SYNC_ENABLE, null,
							null);
			if (c != null && c.moveToFirst()) {
				do {
					long localId = c.getLong(c.getColumnIndex(Sms._ID));
					Uri uri = Uri.withAppendedPath(TSms.CONTENT_URI, String
							.valueOf(localId));
					MmsUtils.enableMessageOrThreadSyncInternal(ctx, uri, true);
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeSyncMMSTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver()
					.query(
							TMms.CONTENT_URI,
							new String[] { Mms._ID, "guid", "sync_state",
									"sync_enable" },
							"sync_enable = " + SyncState.SYNC_ENABLE, null,
							null);
			if (c != null && c.moveToFirst()) {
				do {
					long localId = c.getLong(c.getColumnIndex(Mms._ID));
					Uri uri = Uri.withAppendedPath(TMms.CONTENT_URI, String
							.valueOf(localId));
					MmsUtils.enableMessageOrThreadSyncInternal(ctx, uri, true);
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeFakedTasks() {
			Log.d(TAG, "executeFakedTasks");
			Context ctx = TMmsSyncService.this;
			ctx.getContentResolver().delete(
					TMmsSms.CONTENT_URI,
					"sync_state = '" + SyncState.SYNC_STATE_DELETED + "'"
							+ " OR  sync_state = '"
							+ SyncState.SYNC_STATE_REMOVED + "'"
							+ " OR  sync_state = '" + SyncState.SYNC_STATE_TMP
							+ "'", null);

		}

		private void executeDeleteThreadTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver().query(
					Uri.withAppendedPath(TMmsSms.AUTHORITY_URI, "all-threads"),
					//Uri.parse("content://mms-sms/all-threads/"),
					new String[] { Threads._ID, Threads.RECIPIENT_IDS,
							"sync_state" },
					"sync_state = '" + SyncState.SYNC_STATE_DELETED + "'",
					null, null);
			if (c != null && c.moveToFirst()) {
				do {
					long localId = c.getLong(c.getColumnIndex(Threads._ID));
					
					String address = MessageUtils.getAddressByThreadId(ctx,
							localId);
					Log.d(TAG, "executeDeleteThreadTasks local address " + address);
					Uri uri = Uri.withAppendedPath(TThreads.CONTENT_URI, String
							.valueOf(localId));
					if (!MmsUtils.isThreadSyncable(ctx, localId)) {
//						ctx.getContentResolver().delete(uri, null, null);
//						//add by chenqiang
//						SQLiteOpenHelper mOpenHelper = MmsSmsDatabaseHelper.getInstance(ctx);
//						SQLiteDatabase db = mOpenHelper.getWritableDatabase();
//						ContentValues values = new ContentValues();
//						values.put("sync_state", SyncState.SYNC_STATE_DELETED);
//						db.update("sms", values, "thread_id="+localId, null);
			
					} else if (SyncAction.deleteThread(ctx, address)) {
//						ctx.getContentResolver().delete(uri, null, null);
					}
				} while (c.moveToNext());
			}
			c.close();

		}

		private void executeRemoveThreadTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver().query(
					Uri.withAppendedPath(TMmsSms.AUTHORITY_URI, "all-threads"),
					//Uri.parse("content://mms-sms/all-threads/"),
					new String[] { Threads._ID, Threads.RECIPIENT_IDS,
							"sync_state" },
					"sync_state = '" + SyncState.SYNC_STATE_REMOVED + "'",
					null, null);

			if (c != null && c.moveToFirst()) {
				do {
					long localId = c.getLong(c.getColumnIndex(Threads._ID));
					
					String address = MessageUtils.getAddressByThreadId(ctx,
							localId);
					Log.d(TAG, "executeRemoveThreadTasks address is " + address);
					Uri uri = Uri.withAppendedPath(TThreads.CONTENT_URI, String
							.valueOf(localId));

					if (!MmsUtils.isThreadSyncable(ctx, localId)) {
						ctx.getContentResolver().delete(uri, null, null);
					} else if (SyncAction.finalDeleteThread(ctx, address)) {
						ctx.getContentResolver().delete(uri, null, null);
					}

				} while (c.moveToNext());
			}
			c.close();

		}

		private void executeAddMmsTasks() throws InvalidHeaderValueException {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver().query(
					TMms.CONTENT_URI,
					new String[] { "guid", Mms._ID, "sync_state",
							Mms.MESSAGE_BOX },
					"( guid IS NULL OR guid = -1 ) AND sync_enable = 1", null,
					null);
			if (c != null && c.moveToFirst()) {
				do {
					String localId = c.getString(c.getColumnIndex(Mms._ID));
					try {
						int guid = TaskExecuter.executeAddMmsTask(localId, ctx);
						Uri resultUri = Uri.withAppendedPath(TMms.CONTENT_URI,
								localId);
						Log.d(TAG, "executeAddMmsTasks resultUri :"+resultUri);
						MmsUtils.setGuid(ctx, resultUri, guid);
						MmsUtils.markMessageOrThread(ctx, resultUri,
								SyncState.SYNC_STATE_PRESENT);
						if (c.getInt(c.getColumnIndex(Mms.MESSAGE_BOX)) == Mms.MESSAGE_BOX_INBOX) {
							Uri dataUri = Uri.withAppendedPath(
									Uri.withAppendedPath(TMms.AUTHORITY_URI, "data"),
									//Uri.parse("content://mms/data"), 
									localId);
//							ctx.getContentResolver()
//									.delete(dataUri, null, null);
						}
					} catch (ClientProtocolException e) {
						Log.d(TAG, "ClientProtocolException " + localId);
					} catch (ElementNotFound e) {
						Log.d(TAG, "ElementNotFound " + localId);
					} catch (IOException e) {
						Log.d(TAG, "IOException " + localId);
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeAddSMSTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver().query(TSms.CONTENT_URI,
					new String[] { "guid", Sms._ID, "sync_state" },
					"( guid IS NULL OR guid = -1 ) AND sync_enable = 1", null,
					null);
			if (c != null && c.moveToFirst()) {
				do {
					String localId = c.getString(c.getColumnIndex(Sms._ID));
					try {
						int guid = TaskExecuter.executeAddSMSTask(localId, ctx);
						Uri resultUri = Uri.withAppendedPath(TSms.CONTENT_URI,
								localId);
						Log.d(TAG, "executeAddSMSTasks resultUri :"+resultUri);
						MmsUtils.setGuid(ctx, resultUri, guid);
						MmsUtils.markMessageOrThread(ctx, resultUri,
								SyncState.SYNC_STATE_PRESENT);
					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeRecoverSMSTasks() {
			//Log.d(TAG,"executeRecoverSMSTasks----------start");
			Context ctx = TMmsSyncService.this;
//			Cursor c = ctx.getContentResolver().query(
//					TSms.CONTENT_URI,
//					new String[] { "guid", Sms._ID, "sync_state" },
//					"guid IS NOT NULL AND sync_state = '"
//							+ SyncState.SYNC_STATE_RECOVER + "'", null, null);
			Cursor c = ctx.getContentResolver().query(
					TSms.CONTENT_URI,
					new String[] { "guid", Sms._ID, "sync_state" },
					"guid !=-1 AND sync_state = '"
							+ SyncState.SYNC_STATE_RECOVER + "'", null, null);
			if (c != null && c.moveToFirst()) {
				do {
					String localId = c.getString(c.getColumnIndex(Sms._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					try {
						if (TaskExecuter.executeRecoverTask(guid, ctx)) {
							Uri resultUri = Uri.withAppendedPath(
									TSms.CONTENT_URI, localId);
							Log.d(TAG, "executeRecoverSMSTasks resultUri :"+resultUri);
							MmsUtils.markMessageOrThread(ctx, resultUri,
									SyncState.SYNC_STATE_PRESENT);
						}

					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeRecoverMMSTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver().query(
					TMms.CONTENT_URI,
					new String[] { "guid", Mms._ID, "sync_state" },
					"guid IS NOT NULL AND sync_state = '"
							+ SyncState.SYNC_STATE_RECOVER + "'", null, null);
			if (c != null && c.moveToFirst()) {
				do {
					String localId = c.getString(c.getColumnIndex(Mms._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					try {
						if (TaskExecuter.executeRecoverTask(guid, ctx)) {
							Uri resultUri = Uri.withAppendedPath(
									TMms.CONTENT_URI, localId);
							Log.d(TAG, "executeRecoverMMSTasks resultUri :"+resultUri);
							MmsUtils.markMessageOrThread(ctx, resultUri,
									SyncState.SYNC_STATE_PRESENT);
							MmsUtils.markMessageSyncDirty(ctx, resultUri, 0);
						}

					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeUpdateSMSTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx
					.getContentResolver()
					.query(
							TSms.CONTENT_URI,
							new String[] { "guid", Sms._ID, "sync_state" },
							"guid IS NOT NULL AND guid <> -1 AND sync_dirty = 1 AND sync_enable = 1",
							null, null);
			if (c != null && c.moveToFirst()) {
				do {
					String localId = c.getString(c.getColumnIndex(Sms._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					try {
						if (TaskExecuter.executeUpdateSMSTask(localId, ctx)) {
							Uri resultUri = Uri.withAppendedPath(
									TSms.CONTENT_URI, localId);
							Log.d(TAG, "executeUpdateSMSTasks resultUri :"+resultUri);
							MmsUtils.markMessageSyncDirty(ctx, resultUri, 0);
						}

					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeUpdateMMSTasks() throws InvalidHeaderValueException {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx
					.getContentResolver()
					.query(
							TSms.CONTENT_URI,
							new String[] { "guid", Mms._ID, "sync_state" },
							"guid IS NOT NULL AND guid <> -1 AND sync_dirty = 1 AND sync_enable = 1",
							null, null);
			if (c != null && c.moveToFirst()) {
				do {
					String localId = c.getString(c.getColumnIndex(Mms._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					try {
						if (TaskExecuter.executeUpdateMMSTask(localId, ctx)) {							
							Uri resultUri = Uri.withAppendedPath(
									TMms.CONTENT_URI, localId);
							Log.d(TAG, "executeUpdateMMSTasks resultUri :"+resultUri);
							MmsUtils.markMessageSyncDirty(ctx, resultUri, 0);
						}

					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeDeleteSMSTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver().query(
					TSms.CONTENT_URI,
					new String[] { "guid", Sms._ID, "sync_state" },
					"guid IS NOT NULL AND sync_state = '"
							+ SyncState.SYNC_STATE_DELETED + "'", null, null);
			if (c != null && c.moveToFirst()) {
				do {
					String localId = c.getString(c.getColumnIndex(Sms._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					try {
						Uri resultUri = Uri.withAppendedPath(TSms.CONTENT_URI,
								localId);
						Log.d(TAG, "executeDeleteSMSTasks resultUri :"+resultUri);
						//todo: 需要验证是否同步成功后还要删除本地数据。
						if (TaskExecuter.executeDeleteTask(guid, ctx)) {
//							ctx.getContentResolver().delete(resultUri, null,
//									null);
							//add by chenqiang
							MmsUtils.markMessageOrThread(ctx, resultUri,
									SyncState.SYNC_STATE_REMOTE_DELETE);
							
						}

					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeRemoveSMSTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver().query(
					TSms.CONTENT_URI,
					new String[] { "guid", Sms._ID, "sync_state" },
					"guid IS NOT NULL AND sync_state = '"
							+ SyncState.SYNC_STATE_REMOVED + "'", null, null);
			if (c != null && c.moveToFirst()) {
				do {
					String localId = c.getString(c.getColumnIndex(Sms._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					try {
						Uri resultUri = Uri.withAppendedPath(TSms.CONTENT_URI,
								localId);
						Log.d(TAG, "executeRemoveSMSTasks resultUri :"+resultUri);
						if (TaskExecuter.executeFinalDelete(guid, ctx)) {
							ctx.getContentResolver().delete(resultUri, null,
									null);
						}

					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeDeleteMMSTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver().query(
					TMms.CONTENT_URI,
					new String[] { "guid", Mms._ID, "sync_state" },
					"guid IS NOT NULL AND sync_state = '"
							+ SyncState.SYNC_STATE_DELETED + "'", null, null);
			if (c != null && c.moveToFirst()) {
				do {
					String localId = c.getString(c.getColumnIndex(Mms._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					try {
						Uri resultUri = Uri.withAppendedPath(TMms.CONTENT_URI,
								localId);
						Log.d(TAG, "executeDeleteMMSTasks resultUri :"+resultUri);
						if (TaskExecuter.executeDeleteTask(guid, ctx)) {
//							ctx.getContentResolver().delete(resultUri, null,
//									null);
							//add by chenqiang
							MmsUtils.markMessageOrThread(ctx, resultUri,
									SyncState.SYNC_STATE_REMOTE_DELETE);
						}

					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (c.moveToNext());
			}
			c.close();
		}

		private void executeRemoveMMSTasks() {
			Context ctx = TMmsSyncService.this;
			Cursor c = ctx.getContentResolver().query(
					TMms.CONTENT_URI,
					new String[] { "guid", Mms._ID, "sync_state" },
					"guid IS NOT NULL AND sync_state = '"
							+ SyncState.SYNC_STATE_REMOVED + "'", null, null);
			if (c != null && c.moveToFirst()) {
				do {
					String localId = c.getString(c.getColumnIndex(Mms._ID));
					String guid = c.getString(c.getColumnIndex("guid"));
					try {
						Uri resultUri = Uri.withAppendedPath(TMms.CONTENT_URI,
								localId);
						Log.d(TAG, "executeRemoveMMSTasks resultUri :"+resultUri);
						if (TaskExecuter.executeFinalDelete(guid, ctx)) {
							ctx.getContentResolver().delete(resultUri, null,
									null);
						}

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
