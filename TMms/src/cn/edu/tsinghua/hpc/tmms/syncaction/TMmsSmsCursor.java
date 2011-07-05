package cn.edu.tsinghua.hpc.tmms.syncaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.ClientProtocolException;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.util.Log;
import cn.edu.tsinghua.hpc.syncbroker.ElementNotFound;
import cn.edu.tsinghua.hpc.syncbroker.MessageType;
import cn.edu.tsinghua.hpc.syncbroker.SMSRecord;
import cn.edu.tsinghua.hpc.syncbroker.SMSType;
import cn.edu.tsinghua.hpc.syncbroker.SyncRecord;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TSms;

/**
 * A mutable cursor implementation backed by an array of {@code Object}s. Use
 * {@link #newRow()} to add rows. Automatically expands internal capacity as
 * needed.
 */
public class TMmsSmsCursor extends AbstractCursor {

	private String[] columnNames;
	private static List<Entry<Long, SMSRecord>> data = new ArrayList<Entry<Long, SMSRecord>>();;

	private static int mPageNo = 1;
	private static int mCountPerPage = 5;
	private Context mContext;

	// faked _ID's offset
	public static long BIG_OFFSET = 100000L;

	public static int CACHE_WINDOW_SIZE = 30;

	/**
	 * Constructs a new cursor with the given initial capacity.
	 * 
	 * @param columnNames
	 *            names of the columns, the ordering of which determines column
	 *            ordering elsewhere in this cursor
	 * @param initialCapacity
	 *            in rows
	 */
	public TMmsSmsCursor(Context ctx, String[] columnNames) {
		this.mContext = ctx;
		this.columnNames = columnNames;
	}

	
	public boolean deleteRow() {
		try {
			data.remove(mPos);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public static boolean updateByGuid(int guid, SMSRecord cs) {
		return false;
	}

	public void clearAllSms(ContentResolver resolver) {
		data.clear();
		currentCachedSMS.clear();
		resolver.delete(TSms.CONTENT_URI, "sync_state = '"
				+ SyncState.SYNC_STATE_TMP + "'", null);
		resolver.delete(TMms.CONTENT_URI, "sync_state = '"
				+ SyncState.SYNC_STATE_TMP + "'", null);
		mPageNo = 1;
	}

	public boolean update(SMSRecord cs) {
		return false;
	}

	@Override
	public boolean requery() {
		// XXX not supported now
		// do nothing now
		// throw new UnsupportedOperationException();
		return true;
	}

	private SMSRecord getSMSRecordByIndex(int index) {
		return data.get(index).getValue();

	}

	private List<SyncRecord> currentCachedSMS = new ArrayList<SyncRecord>();

	/**
	 * temporily insert the Mms-SMS back to database
	 */
	public void cacheSomeMms(List<Long> ids, ContentResolver resolver) {
		Log.d("Mms", "cacheSomeMms");

		Looper.prepare();
		while (currentCachedSMS.size() + ids.size() > CACHE_WINDOW_SIZE) {
			SMSRecord s = (SMSRecord) currentCachedSMS.get(0);
			if (s.getMtype() == MessageType.SMS) {
				resolver.delete(TSms.CONTENT_URI, "_ID is " + s._id
						+ " AND sync_state = '" + SyncState.SYNC_STATE_TMP
						+ "'", null);
			} else {
				resolver.delete(TMms.CONTENT_URI, "_ID is " + s._id
						+ " AND sync_state = '" + SyncState.SYNC_STATE_TMP
						+ "'", null);
			}
			s._id = SyncRecord.ID_IN_MEMORY;
			currentCachedSMS.remove(0);
		}

		// add useful people
		for (long id : ids) {
			Log.d("Mms", "cache id " + id);
			SMSRecord cs = getSMSRecordByIndex((int) (id - BIG_OFFSET));
			if (cs._id == SyncRecord.ID_IN_MEMORY) {
				Uri uri = MmsUtils.insertOneMessageInto(mContext, cs,
						SyncState.SYNC_STATE_TMP);
				cs._id = (int) ContentUris.parseId(uri);
				currentCachedSMS.add(cs);
			}
		}
	}

	/**
	 * temporily insert the Mms-SMS back to database
	 */
	public void cacheSomeRecords(List<SyncRecord> records,
			ContentResolver resolver) {
		Log.d("Mms", "cacheSomeRecords " + records.size());

		while (currentCachedSMS.size() + records.size() > CACHE_WINDOW_SIZE) {
			SMSRecord s = (SMSRecord) currentCachedSMS.get(0);
			if (s.getMtype() == MessageType.SMS) {
				resolver.delete(TSms.CONTENT_URI, "_ID is " + s._id
						+ " AND sync_state = '" + SyncState.SYNC_STATE_TMP
						+ "'", null);
			} else {
				resolver.delete(TMms.CONTENT_URI, "_ID is " + s._id
						+ " AND sync_state = '" + SyncState.SYNC_STATE_TMP
						+ "'", null);
			}
			s._id = SyncRecord.ID_IN_MEMORY;
			currentCachedSMS.remove(0);
		}

		// add useful people
		for (SyncRecord sr : records) {
			Log.d("Mms", "cache content " + sr.getGuid());
			SMSRecord cs = (SMSRecord) sr;
			if (cs._id == SyncRecord.ID_IN_MEMORY) {
				Uri uri = MmsUtils.insertOneMessageInto(mContext, cs,
						SyncState.SYNC_STATE_TMP);
				cs._id = (int) ContentUris.parseId(uri);
				currentCachedSMS.add(cs);
			}
		}
	}

	/**
	 * Gets value at the given column for the current row.
	 */
	private Object get(int column) {
		if (column < 0 || column >= columnNames.length) {
			throw new CursorIndexOutOfBoundsException("Requested column: "
					+ column + ", # of columns: " + columnNames.length);
		}
		if (mPos < 0) {
			throw new CursorIndexOutOfBoundsException("Before first row.");
		}
		if (mPos >= data.size()) {
			throw new CursorIndexOutOfBoundsException("After last row.");
		}

		SMSRecord cs = getSMSRecordByIndex(mPos);
		if (cs == null) {
			return null;
		}
		String c = this.columnNames[column];

		Cursor mmsCursor = null;
		if (cs.getMtype() == MessageType.MMS) {
			if (cs._id == SMSRecord.ID_IN_MEMORY) {
				List<SyncRecord> sr = new ArrayList<SyncRecord>();
				sr.add(cs);
				this.cacheSomeRecords(sr,
						mContext.getContentResolver());
				
			} else {
				Uri mmsUri = Uri.withAppendedPath(TMms.CONTENT_URI, String
						.valueOf(cs._id));
				mmsCursor = mContext.getContentResolver().query(mmsUri,
						MMS_PROJECTION, null, null, null);

			}
		}

		if (BaseColumns._ID.equals(c)) {
			if (cs._id == SMSRecord.ID_IN_MEMORY) {
				return mPos + BIG_OFFSET;
			} else {
				return cs._id;
			}
		} else if ("guid".equals(c)) {
			return cs.getGuid();
		} else if (Conversations.THREAD_ID.equals(c)) {
			return -1;
		} else if (MmsSms.TYPE_DISCRIMINATOR_COLUMN.equals(c)) {
			if (cs.getMtype() == MessageType.SMS) {
				return "sms";
			} else {
				return "mms";
			}
		} else if (Sms.ADDRESS.equals(c)) {
			if (cs.getType() == SMSType.RECEIVE) {
				return cs.getFrom();
			} else {
				return cs.getTo();
			}

		} else if (Sms.BODY.equals(c)) {
			return cs.getBody();
		} else if (Sms.DATE.equals(c)) {
			return cs.getDate().getTime();
		} else if (Sms.READ.equals(c)) {
			return 1;
		} else if (Sms.TYPE.equals(c)) {
			if (cs.getType() == SMSType.RECEIVE) {
				return 1;
			} else {
				return 2;
			}
		} else if (Sms.STATUS.equals(c)) {
			return -1;
		} else if (Mms.SUBJECT.equals(c)) {
			return mmsCursor.getString(mmsCursor.getColumnIndex(Mms.SUBJECT));
		} else if (Mms.SUBJECT_CHARSET.equals(c)) {
			return mmsCursor.getInt(mmsCursor
					.getColumnIndex(Mms.SUBJECT_CHARSET));
		} else if (Mms.DATE.equals(c)) {
			return mmsCursor.getLong(mmsCursor.getColumnIndex(Mms.DATE));
		} else if (Mms.READ.equals(c)) {
			return mmsCursor.getInt(mmsCursor.getColumnIndex(Mms.READ));
		} else if (Mms.MESSAGE_TYPE.equals(c)) {
			return mmsCursor.getInt(mmsCursor.getColumnIndex(Mms.MESSAGE_TYPE));
		} else if (Mms.MESSAGE_BOX.equals(c)) {
			return mmsCursor.getInt(mmsCursor.getColumnIndex(Mms.MESSAGE_BOX));
		} else if (Mms.DELIVERY_REPORT.equals(c)) {
			return mmsCursor.getInt(mmsCursor
					.getColumnIndex(Mms.DELIVERY_REPORT));
		} else if (Mms.READ_REPORT.equals(c)) {
			return mmsCursor.getInt(mmsCursor.getColumnIndex(Mms.READ_REPORT));
		} else if (PendingMessages.ERROR_TYPE.equals(c)) {
			return mmsCursor.getInt(mmsCursor
					.getColumnIndex(PendingMessages.ERROR_TYPE));
		}

		return null;
	}

	public static final String[] PROJECTION = new String[] {
			// TODO: should move this symbol into android.provider.Telephony.
			MmsSms.TYPE_DISCRIMINATOR_COLUMN,
			BaseColumns._ID,
			Conversations.THREAD_ID,
			// For SMS
			Sms.ADDRESS, Sms.BODY, Sms.DATE, Sms.READ, Sms.TYPE,
			Sms.STATUS,
			// For MMS
			Mms.SUBJECT, Mms.SUBJECT_CHARSET, Mms.DATE, Mms.READ,
			Mms.MESSAGE_TYPE, Mms.MESSAGE_BOX, Mms.DELIVERY_REPORT,
			Mms.READ_REPORT, PendingMessages.ERROR_TYPE };

	public static final String[] MMS_PROJECTION = new String[] {
			MmsSms.TYPE_DISCRIMINATOR_COLUMN, BaseColumns._ID,
			Conversations.THREAD_ID, Mms.SUBJECT, Mms.SUBJECT_CHARSET,
			Mms.DATE, Mms.READ, Mms.MESSAGE_TYPE, Mms.MESSAGE_BOX,
			Mms.DELIVERY_REPORT, Mms.READ_REPORT, PendingMessages.ERROR_TYPE };

	// AbstractCursor implementation.

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public String[] getColumnNames() {
		return columnNames;
	}

	public void setCoulumnNames(String[] columns) {
		this.columnNames = columns;
	}

	@Override
	public String getString(int column) {
		Object value = get(column);
		if (value == null)
			return null;
		return value.toString();
	}

	@Override
	public short getShort(int column) {
		Object value = get(column);
		if (value == null)
			return 0;
		if (value instanceof Number)
			return ((Number) value).shortValue();
		return Short.parseShort(value.toString());
	}

	@Override
	public int getInt(int column) {
		Object value = get(column);
		if (value == null)
			return 0;
		if (value instanceof Number)
			return ((Number) value).intValue();
		return Integer.parseInt(value.toString());
	}

	@Override
	public long getLong(int column) {
		Object value = get(column);
		if (value == null)
			return 0;
		if (value instanceof Number)
			return ((Number) value).longValue();
		return Long.parseLong(value.toString());
	}

	@Override
	public float getFloat(int column) {
		Object value = get(column);
		if (value == null)
			return 0.0f;
		if (value instanceof Number)
			return ((Number) value).floatValue();
		return Float.parseFloat(value.toString());
	}

	@Override
	public double getDouble(int column) {
		Object value = get(column);
		if (value == null)
			return 0.0d;
		if (value instanceof Number)
			return ((Number) value).doubleValue();
		return Double.parseDouble(value.toString());
	}

	@Override
	public boolean isNull(int column) {
		return get(column) == null;
	}

	public void retrieveArchivedSMS(String query, Handler handler) {
		new Timer().schedule(new RetriveAchivedSMSTimerTask(query, handler), 0);
	}

	private class RetriveAchivedSMSTimerTask extends TimerTask {

		int count;
		String queryString;
		Handler handler;

		public RetriveAchivedSMSTimerTask(String query, Handler handler) {
			count = mCountPerPage;
			queryString = query;
			this.handler = handler;
		}

		@Override
		public void run() {
			try {
				List<SyncRecord> result = SyncAction.retriveArchievedSMS(
						mContext, mPageNo, count, queryString);

				for (SyncRecord s : result) {
					data.add(new MapEntry(new Long(s.getGuid()), s));
				}
				Log.d("Mms", "data size is " + result.size());
				if (result.size() == 0) {

				} else if (result.size() < count) {
					mPageNo++;
				} else {
					mPageNo++;
				}

				if (this.handler != null) {
					handler.sendEmptyMessage(0);
				}
				cacheSomeRecords(result, mContext.getContentResolver());

			} catch (ClientProtocolException e) {
				Log.d("Mms", "ClientProtocolException");
			} catch (ElementNotFound e) {
				Log.d("Mms", "ElementNotFound");
			} catch (IOException e) {
				Log.d("Mms", "IOException");
				this.cancel();
			}

		}

	}

	/**
	 * this class is copied from java.util.MapEntry, for its visibility is
	 * internal
	 */
	class MapEntry<K, V> implements Map.Entry<K, V>, Cloneable {

		K key;
		V value;

		MapEntry(K theKey) {
			key = theKey;
		}

		MapEntry(K theKey, V theValue) {
			key = theKey;
			value = theValue;
		}

		@Override
		public Object clone() {
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (object instanceof Map.Entry) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) object;
				return (key == null ? entry.getKey() == null : key.equals(entry
						.getKey()))
						&& (value == null ? entry.getValue() == null : value
								.equals(entry.getValue()));
			}
			return false;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		@Override
		public int hashCode() {
			return (key == null ? 0 : key.hashCode())
					^ (value == null ? 0 : value.hashCode());
		}

		public V setValue(V object) {
			V result = value;
			value = object;
			return result;
		}

		@Override
		public String toString() {
			return key + "=" + value;
		}
	}
}
