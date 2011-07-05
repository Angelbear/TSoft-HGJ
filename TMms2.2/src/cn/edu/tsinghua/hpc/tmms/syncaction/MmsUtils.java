package cn.edu.tsinghua.hpc.tmms.syncaction;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Inbox;
import android.provider.Telephony.Sms.Sent;
import android.provider.Telephony.Threads;
import android.util.Log;
import cn.edu.tsinghua.hpc.google.tmms.MmsException;
import cn.edu.tsinghua.hpc.google.tmms.pdu.GenericPdu;
import cn.edu.tsinghua.hpc.google.tmms.pdu.PduParser;
import cn.edu.tsinghua.hpc.google.tmms.pdu.PduPersister;
import cn.edu.tsinghua.hpc.google.tmms.util.SqliteWrapper;
import cn.edu.tsinghua.hpc.syncbroker.MessageType;
import cn.edu.tsinghua.hpc.syncbroker.SMSRecord;
import cn.edu.tsinghua.hpc.tmms.service.BootReceiver;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMmsSms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TSms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TThreads;


public class MmsUtils {

	public static void notifySyncService(Context ctx) {
		Intent i = new Intent(ctx, BootReceiver.class);
		i.setAction(BootReceiver.TSYNC_REQUEST);
		ctx.sendBroadcast(i);
	}

	public static int setGuid(Context ctx, Uri uri, int guid) {
		ContentValues values = new ContentValues();
		values.put("guid", guid);
		return ctx.getContentResolver().update(uri, values, null, null);
	}

	public static int markMessageOrThread(Context ctx, Uri uri, String syncState) {
		ContentValues values = new ContentValues();
		values.put("sync_state", syncState);
		return ctx.getContentResolver().update(uri, values, null, null);
	}

	public static int markMessageSyncDirty(Context ctx, Uri uri, int dirty) {
		// Mark the sync_dirty bits of the messages true
		ContentValues values = new ContentValues();
		values.put("sync_dirty", dirty);
		return ctx.getContentResolver().update(uri, values, null, null);
	}

	// sync_state: 
	// 0 disabled
	// 1 enabled
	// 2 disable
	// 3 enable
	// 0 -> 3 (service) -> 1 -> 2 (service) -> 0
	//      |                   |
	//      0                   1
	public static int enableMessageOrThreadSync(Context ctx, Uri uri,
			boolean sync) {
		ContentValues values = new ContentValues();

		int state = isMessageSyncableInternal(ctx, uri);
		Log.d("MmsUtils", "uri is " + uri.toString() + " state is " + state);
		if (sync) {
			if (state == SyncState.SYNC_DISABLED) {
				values.put("sync_enable", SyncState.SYNC_ENABLE);
			} else if (state == SyncState.SYNC_DISABLE) {
				values.put("sync_enable", SyncState.SYNC_ENABLED);
			} else {
				return 0;
			}
		} else {
			if (state == SyncState.SYNC_ENABLED) {
				values.put("sync_enable", SyncState.SYNC_DISABLE);
			} else if (state == SyncState.SYNC_ENABLE) {
				values.put("sync_enable", SyncState.SYNC_DISABLED);
			} else {
				return 0;
			}
		}
		return ctx.getContentResolver().update(uri, values, null, null);
	}

	public static int enableMessageOrThreadSyncInternal(Context ctx, Uri uri,
			boolean sync) {
		ContentValues values = new ContentValues();
		if (sync) {
			values.put("sync_enable", SyncState.SYNC_ENABLED);
		} else {
			values.put("sync_enable", SyncState.SYNC_DISABLED);
		}
		return ctx.getContentResolver().update(uri, values, null, null);
	}

	public static boolean isMessageSyncable(Context ctx, Uri uri) {
		Cursor c = ctx.getContentResolver().query(uri,
				new String[] { "sync_enable" }, null, null, null);
		if (c != null && c.moveToFirst()) {
			int state = c.getInt(c.getColumnIndex("sync_enable"));
			c.close();
			return state == SyncState.SYNC_ENABLED
					|| state == SyncState.SYNC_DISABLE;
		}
		c.close();
		return false;
	}

	public static int isMessageSyncableInternal(Context ctx, Uri uri) {
		Cursor c = ctx.getContentResolver().query(uri,
				new String[] { "sync_enable" }, null, null, null);
		if (c != null && c.moveToFirst()) {
			return c.getInt(c.getColumnIndex("sync_enable"));
		}
		c.close();
		return 0;
	}

	public static boolean hasThreadLockedMessages(Context ctx, long threadId) {
		Uri uri = ContentUris.withAppendedId(TThreads.CONTENT_URI, threadId);
		if (threadId == -1) {
			uri = TThreads.CONTENT_URI;
		}
		Cursor c = ctx.getContentResolver().query(uri,
				new String[] { "locked" }, "locked=1", null, null);
		boolean bool = c.getCount() != 0;
		c.close();
		return bool;
	}

	public static boolean isThreadSyncable(Context ctx, long threadId) {
		Uri uri = ContentUris.withAppendedId(
				Uri.withAppendedPath(TMmsSms.AUTHORITY_URI, "threads"),
				//Uri.parse("content://mms-sms/threads/"), 
				threadId);
		return isMessageSyncable(ctx, uri);
	}

	public static Uri insertOneMessageInto(Context ctx, SMSRecord s,
			String syncState) {
		Uri resultUri = null;
		if (s == null)
			return null;

		if (s.getMtype() == MessageType.SMS) {
			ContentValues values = new ContentValues();
			switch (s.getType()) {
			case RECEIVE:
				values.put(Inbox.ADDRESS, s.getFrom());
				values.put(Inbox.BODY, s.getBody());
				values.put(Inbox.PROTOCOL, Integer.valueOf(0));
				values.put(Inbox.REPLY_PATH_PRESENT, Integer.valueOf(0));
				values.put(Inbox.READ, Integer.valueOf(1));
				values.put(Inbox.DATE, s.getDate().getTime());
				values.put("guid", s.getGuid());
				values.put("sync_state", syncState);
				resultUri = SqliteWrapper.insert(ctx, ctx.getContentResolver(),
						TSms.TInbox.CONTENT_URI, values);
				break;
			case SEND:
				values.put(Sent.ADDRESS, s.getTo());
				values.put(Sent.BODY, s.getBody());
				values.put(Sent.PROTOCOL, Integer.valueOf(0));
				values.put(Sent.REPLY_PATH_PRESENT, Integer.valueOf(0));
				values.put(Sent.READ, Integer.valueOf(1));
				values.put(Sent.DATE, s.getDate().getTime());
				values.put("guid", s.getGuid());
				values.put("sync_state", syncState);
				resultUri = SqliteWrapper.insert(ctx, ctx.getContentResolver(),
						TSms.TSent.CONTENT_URI, values);
				break;
			default:
				break;
			}
		} else if (s.getMtype() == MessageType.MMS) {
			PduPersister p = PduPersister.getPduPersister(ctx);
			GenericPdu pdu;
			switch (s.getType()) {
			case RECEIVE:
				pdu = new PduParser(StringEncoder.decode(s.getPdu())).parse();
				try {
					resultUri = p.persist(pdu, TMms.TInbox.CONTENT_URI);
					setGuid(ctx, resultUri, s.getGuid());
					markMessageOrThread(ctx, resultUri, syncState);
				} catch (MmsException e) {
				}
				break;
			case SEND:
				pdu = new PduParser(StringEncoder.decode(s.getPdu())).parse();
				try {
					resultUri = p.persist(pdu, TMms.TSent.CONTENT_URI);
					setGuid(ctx, resultUri, s.getGuid());
					markMessageOrThread(ctx, resultUri, syncState);
				} catch (MmsException e) {
				}
				break;
			}

		}

		if (resultUri != null) {
			Cursor mCursor = ctx.getContentResolver().query(resultUri,
					new String[] { Sms.THREAD_ID, Sms.DATE }, null, null,
					Sms.DATE + " DESC");
			if (mCursor != null && mCursor.moveToFirst()) {
				int thread_id = mCursor.getInt(mCursor
						.getColumnIndex(Sms.THREAD_ID));
				long lastDate = mCursor.getLong(mCursor
						.getColumnIndex(Sms.DATE));

				Uri mThread = Uri.withAppendedPath(Uri.withAppendedPath(
						TMmsSms.CONTENT_URI, "threads"), String
						.valueOf(thread_id));
				ContentValues threadValues = new ContentValues();
				threadValues.put(Threads.DATE, lastDate);
				//add by chenqiang
				threadValues.put("sync_state", syncState);
				ctx.getContentResolver().update(mThread, threadValues, null,
						null);
				mCursor.close();
			}
			mCursor.close();
		}

		return resultUri;
	}
	
	
	public static Uri inserRemoteThreadInto(Context ctx, SMSRecord s,
			String syncState) {
		Uri resultUri = null;
		if (s == null)
			return null;

		if (s.getMtype() == MessageType.SMS) {
			ContentValues values = new ContentValues();
			switch (s.getType()) {
			case RECEIVE:
				values.put(Inbox.ADDRESS, s.getFrom());
				values.put(Inbox.BODY, s.getBody());
				values.put(Inbox.PROTOCOL, Integer.valueOf(0));
				values.put(Inbox.REPLY_PATH_PRESENT, Integer.valueOf(0));
				values.put(Inbox.READ, Integer.valueOf(1));
				values.put(Inbox.DATE, s.getDate().getTime());
				values.put("guid", s.getGuid());
				values.put("sync_state", syncState);
				resultUri = SqliteWrapper.insert(ctx, ctx.getContentResolver(),
						TSms.TInbox.CONTENT_URI, values);
				break;
			case SEND:
				values.put(Sent.ADDRESS, s.getTo());
				values.put(Sent.BODY, s.getBody());
				values.put(Sent.PROTOCOL, Integer.valueOf(0));
				values.put(Sent.REPLY_PATH_PRESENT, Integer.valueOf(0));
				values.put(Sent.READ, Integer.valueOf(1));
				values.put(Sent.DATE, s.getDate().getTime());
				values.put("guid", s.getGuid());
				values.put("sync_state", syncState);
				resultUri = SqliteWrapper.insert(ctx, ctx.getContentResolver(),
						TSms.TSent.CONTENT_URI, values);
				break;
			default:
				break;
			}
		} else if (s.getMtype() == MessageType.MMS) {
			PduPersister p = PduPersister.getPduPersister(ctx);
			GenericPdu pdu;
			switch (s.getType()) {
			case RECEIVE:
				pdu = new PduParser(StringEncoder.decode(s.getPdu())).parse();
				try {
					resultUri = p.persist(pdu, TMms.TInbox.CONTENT_URI);
					setGuid(ctx, resultUri, s.getGuid());
					markMessageOrThread(ctx, resultUri, syncState);
				} catch (MmsException e) {
				}
				break;
			case SEND:
				pdu = new PduParser(StringEncoder.decode(s.getPdu())).parse();
				try {
					resultUri = p.persist(pdu, TMms.TSent.CONTENT_URI);
					setGuid(ctx, resultUri, s.getGuid());
					markMessageOrThread(ctx, resultUri, syncState);
				} catch (MmsException e) {
				}
				break;
			}

		}

		if (resultUri != null) {
			Cursor mCursor = ctx.getContentResolver().query(resultUri,
					new String[] { Sms.THREAD_ID, Sms.DATE }, null, null,
					Sms.DATE + " DESC");
			if (mCursor != null && mCursor.moveToFirst()) {
				int thread_id = mCursor.getInt(mCursor
						.getColumnIndex(Sms.THREAD_ID));
				long lastDate = mCursor.getLong(mCursor
						.getColumnIndex(Sms.DATE));

				Uri mThread = Uri.withAppendedPath(Uri.withAppendedPath(
						TMmsSms.CONTENT_URI, "threads"), String
						.valueOf(thread_id));
				ContentValues threadValues = new ContentValues();
				threadValues.put(Threads.DATE, lastDate);
				threadValues.put("sync_state", syncState);
				ctx.getContentResolver().update(mThread, threadValues, null,
						null);
				mCursor.close();
			}
			mCursor.close();
		}

		return resultUri;
	}
	
	
	public static Uri tempInsertOneThreadInto(Context ctx, SMSRecord s) {
		Uri resultUri = null;
		if (s == null)
			return null;

		if (s.getMtype() == MessageType.SMS) {
			ContentValues values = new ContentValues();
			switch (s.getType()) {
			case RECEIVE:
				values.put(Inbox.ADDRESS, s.getFrom());
				values.put(Inbox.BODY, s.getBody());
				values.put(Inbox.PROTOCOL, Integer.valueOf(0));
				values.put(Inbox.REPLY_PATH_PRESENT, Integer.valueOf(0));
				values.put(Inbox.READ, Integer.valueOf(1));
				values.put(Inbox.DATE, s.getDate().getTime());
				values.put("guid", s.getGuid());
				values.put("sync_state", SyncState.SYNC_STATE_TMP);
				resultUri = SqliteWrapper.insert(ctx, ctx.getContentResolver(),
						TSms.TInbox.CONTENT_URI, values);
				break;
			case SEND:
				values.put(Sent.ADDRESS, s.getTo());
				values.put(Sent.BODY, s.getBody());
				values.put(Sent.PROTOCOL, Integer.valueOf(0));
				values.put(Sent.REPLY_PATH_PRESENT, Integer.valueOf(0));
				values.put(Sent.READ, Integer.valueOf(1));
				values.put(Sent.DATE, s.getDate().getTime());
				values.put("guid", s.getGuid());
				values.put("sync_state", SyncState.SYNC_STATE_TMP);
				resultUri = SqliteWrapper.insert(ctx, ctx.getContentResolver(),
						TSms.TSent.CONTENT_URI, values);
				break;
			default:
				break;
			}
		} else if (s.getMtype() == MessageType.MMS) {
			PduPersister p = PduPersister.getPduPersister(ctx);
			GenericPdu pdu;
			switch (s.getType()) {
			case RECEIVE:
				pdu = new PduParser(StringEncoder.decode(s.getPdu())).parse();

				try {
					resultUri = p.persist(pdu, TMms.TInbox.CONTENT_URI);
					setGuid(ctx, resultUri, s.getGuid());
					markMessageOrThread(ctx, resultUri,
							SyncState.SYNC_STATE_TMP);
				} catch (MmsException e) {
				}
				break;
			case SEND:
				pdu = new PduParser(StringEncoder.decode(s.getPdu())).parse();
				try {
					resultUri = p.persist(pdu, TMms.TSent.CONTENT_URI);
					setGuid(ctx, resultUri, s.getGuid());
					markMessageOrThread(ctx, resultUri,
							SyncState.SYNC_STATE_TMP);
				} catch (MmsException e) {
				}
				break;
			}

		}
		if (resultUri != null) {

			Cursor mCursor = ctx.getContentResolver().query(resultUri,
					new String[] { Sms.THREAD_ID, Sms.DATE, Mms.THREAD_ID },
					null, null, Sms.DATE + " DESC");
			if (mCursor != null && mCursor.moveToFirst()) {
				int thread_id = mCursor.getInt(mCursor
						.getColumnIndex(Sms.THREAD_ID));
				long lastDate = mCursor.getLong(mCursor
						.getColumnIndex(Sms.DATE));
				Uri mThread = Uri.withAppendedPath(Uri.withAppendedPath(
						TMmsSms.CONTENT_URI, "threads"), String
						.valueOf(thread_id));
				ContentValues threadValues = new ContentValues();
				threadValues.put(Threads.DATE, lastDate);
				ctx.getContentResolver().update(mThread, threadValues, null,
						null);
				resultUri = mThread;
				mCursor.close();
			}
			mCursor.close();
		}
		

		return resultUri;
	}

}
