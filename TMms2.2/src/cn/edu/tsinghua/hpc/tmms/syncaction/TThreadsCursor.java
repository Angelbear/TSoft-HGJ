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
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Handler;
import android.provider.Telephony.Threads;
import android.util.Log;
import cn.edu.tsinghua.hpc.syncbroker.ElementNotFound;
import cn.edu.tsinghua.hpc.syncbroker.SMSRecord;
import cn.edu.tsinghua.hpc.syncbroker.SMSType;
import cn.edu.tsinghua.hpc.syncbroker.SyncRecord;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TSms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TThreads;

/**
 * A mutable cursor implementation backed by an array of {@code Object}s. Use
 * {@link #newRow()} to add rows. Automatically expands internal capacity as
 * needed.
 */
public class TThreadsCursor extends AbstractCursor {

	private String[] columnNames;
	private static List<Entry<Long, SMSRecord>> data = new ArrayList<Entry<Long, SMSRecord>>();;

	private static int mPageNo = 1;
	private static int mCountPerPage = 5;
	private Context mContext;

	// faked _ID's offset
	public static long BIG_OFFSET = 100000L;

	public static int CACHE_WINDOW_SIZE = 20;

	/**
	 * Constructs a new cursor with the given initial capacity.
	 * 
	 * @param columnNames
	 *            names of the columns, the ordering of which determines column
	 *            ordering elsewhere in this cursor
	 * @param initialCapacity
	 *            in rows
	 */
	public TThreadsCursor(Context ctx, String[] columnNames) {
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

		@Override
	public boolean requery() {
		// XXX not supported now
		// do nothing now
		// throw new UnsupportedOperationException();
		return true;
	}

	private SMSRecord getThreadInfoByIndex(int index) {
		return data.get(index).getValue();

	}

	private List<SyncRecord> currentCachedThread = new ArrayList<SyncRecord>();

	/**
	 * temporily insert the Mms-SMS back to database
	 */
	public void cacheSomeThreads(List<Long> ids, ContentResolver resolver) {

		Log.d("Mms", "cacheSomeThreads");

		while (currentCachedThread.size() + ids.size() > CACHE_WINDOW_SIZE) {
			SyncRecord s = currentCachedThread.get(0);
			resolver.delete(TSms.CONTENT_URI, "_ID is " + s._id
					+ " AND guid IS " + s.getGuid(), null);
			s._id = SyncRecord.ID_IN_MEMORY;
			currentCachedThread.remove(0);
		}

		// add useful people
		for (long id : ids) {
			Log.d("Mms", "cache id " + id);
			SMSRecord cs = getThreadInfoByIndex((int) (id - BIG_OFFSET));
			if (cs._id == SyncRecord.ID_IN_MEMORY) {
				Uri uri = MmsUtils.insertOneMessageInto(mContext, cs,
						SyncState.SYNC_STATE_TMP);
				cs._id = (int) ContentUris.parseId(uri);
				currentCachedThread.add(cs);
			}
		}
	}

	/**
	 * temporily insert the Mms-SMS back to database
	 */
	public void cacheSomeThreadsByRecord(List<SyncRecord> records,
			ContentResolver resolver) {
		Log.d("Mms", "cacheSomeThreadsByRecord");

		while (currentCachedThread.size() + records.size() > CACHE_WINDOW_SIZE) {
			SyncRecord s = currentCachedThread.get(0);
			resolver.delete(TThreads.CONTENT_URI, "_ID is " + s._id
					+ " AND cached IS 0", null);
			s._id = SyncRecord.ID_IN_MEMORY;
			currentCachedThread.remove(0);
		}

		// add useful people
		for (SyncRecord sr : records) {
			Log.d("Mms", "cache id " + sr._id);
			SMSRecord cs = (SMSRecord) sr;
			if (cs._id == SyncRecord.ID_IN_MEMORY) {
				Uri uri = MmsUtils.tempInsertOneThreadInto(mContext, cs);
				cs._id = (int) ContentUris.parseId(uri);
				currentCachedThread.add(cs);
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

		SMSRecord cs = getThreadInfoByIndex(mPos);
		if (cs == null) {
			return null;
		}
		String c = this.columnNames[column];

		if (Threads._ID.equals(c)) {
			if (cs._id == SMSRecord.ID_IN_MEMORY) {
				return mPos + BIG_OFFSET;
			} else {
				return cs._id;
			}
		} else if (Threads.MESSAGE_COUNT.equals(c)) {
			return 1;
		} else if (Threads.RECIPIENT_IDS.equals(c)) {
			if(cs.getType()== SMSType.RECEIVE)
			{
				return cs.getFrom();
			}else
			{
				return cs.getTo();
			}
		} else if (Threads.DATE.equals(c)) {
			return cs.getDate().getTime();
		} else if (Threads.READ.equals(c)) {
			return 1;
		} else if (Threads.SNIPPET.equals(c)) {
			return cs.getBody();
		} else if (Threads.SNIPPET_CHARSET.equals(c)) {
			return 0;
		} else if (Threads.ERROR.equals(c)) {
			// XXX
			return 0;
		} else if (Threads.HAS_ATTACHMENT.equals(c)) {
			// XXX
			return 0;
		}

		return null;
	}

	static final String[] PROJECTION = new String[] { Threads._ID, // 0
			Threads.MESSAGE_COUNT, // 1
			Threads.RECIPIENT_IDS, // 2
			Threads.DATE, // 3
			Threads.READ, // 4
			Threads.SNIPPET, // 5
			Threads.SNIPPET_CHARSET, // 6
			Threads.ERROR, // 7
			Threads.HAS_ATTACHMENT // 8
	};

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

	public void retrieveArchivedThreads(Handler handler) {
		new Timer().schedule(new RetriveAchivedSMSTimerTask(handler), 0);
	}

	private class RetriveAchivedSMSTimerTask extends TimerTask {

		int count;
		Handler handler;

		public RetriveAchivedSMSTimerTask(Handler handler) {
			count = mCountPerPage;
			this.handler = handler;
		}

		@Override
		public void run() {
			Log.d("Mms", "retrieveArchivedSMS task began");
			try {
				List<SyncRecord> result = SyncAction.retriveAchivedThread(
						mContext, mPageNo, count);

				for (SyncRecord s : result) {
					data.add(new MapEntry(new Long(s.getGuid()), s));
				}

				if (result.size() == 0) {

				} else if (result.size() < count) {
					mPageNo++;
				} else {
					mPageNo++;
				}

				if (this.handler != null) {
					handler.sendEmptyMessage(0);
				}

				cacheSomeThreadsByRecord(result, mContext.getContentResolver());

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
