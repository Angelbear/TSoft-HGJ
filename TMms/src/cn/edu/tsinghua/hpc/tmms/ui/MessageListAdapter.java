/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.util.Config;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import cn.edu.tsinghua.hpc.google.tmms.MmsException;
import cn.edu.tsinghua.hpc.tmms.R;
import cn.edu.tsinghua.hpc.tmms.syncaction.TMmsSmsCursor;


/**
 * The back-end data adapter of a message list.
 */
public class MessageListAdapter extends CursorAdapter {
	private static final String TAG = "MessageListAdapter";
	private static final boolean DEBUG = false;
	private static final boolean LOCAL_LOGV = Config.LOGV && DEBUG;

	static final String[] PROJECTION = new String[] {
			// TODO: should move this symbol into android.provider.Telephony.
			MmsSms.TYPE_DISCRIMINATOR_COLUMN,
			BaseColumns._ID,
			Conversations.THREAD_ID,
			"guid",
			"sync_state",
			// For SMS
			Sms.ADDRESS, Sms.BODY, Sms.DATE, Sms.READ, Sms.TYPE,
			Sms.STATUS,
			Sms.LOCKED,
			// For MMS
			Mms.SUBJECT, Mms.SUBJECT_CHARSET, Mms.DATE, Mms.READ,
			Mms.MESSAGE_TYPE, Mms.MESSAGE_BOX, Mms.DELIVERY_REPORT,
			Mms.READ_REPORT, PendingMessages.ERROR_TYPE, Mms.LOCKED };

	// The indexes of the default columns which must be consistent
	// with above PROJECTION.
	static final int COLUMN_MSG_TYPE = 0;
	static final int COLUMN_ID = 1;
	static final int COLUMN_THREAD_ID = 2;
	static final int COLUMN_GUID = 3;
	static final int COLUMN_SYNC_STATE = 4;
	static final int COLUMN_SMS_ADDRESS = 5;
	static final int COLUMN_SMS_BODY = 6;
	static final int COLUMN_SMS_DATE = 7;
	static final int COLUMN_SMS_READ = 8;
	static final int COLUMN_SMS_TYPE = 9;
	static final int COLUMN_SMS_STATUS = 10;
	static final int COLUMN_SMS_LOCKED = 11;
	static final int COLUMN_MMS_SUBJECT = 12;
	static final int COLUMN_MMS_SUBJECT_CHARSET = 13;
	static final int COLUMN_MMS_DATE = 14;
	static final int COLUMN_MMS_READ = 15;
	static final int COLUMN_MMS_MESSAGE_TYPE = 16;
	static final int COLUMN_MMS_MESSAGE_BOX = 17;
	static final int COLUMN_MMS_DELIVERY_REPORT = 18;
	static final int COLUMN_MMS_READ_REPORT = 19;
	static final int COLUMN_MMS_ERROR_TYPE = 20;
	static final int COLUMN_MMS_LOCKED = 21;

	private static final int CACHE_SIZE = 50;

	protected LayoutInflater mInflater;
	private final ListView mListView;
	private final LinkedHashMap<Long, MessageItem> mMessageItemCache;
	private final ColumnsMap mColumnsMap;
	private OnDataSetChangedListener mOnDataSetChangedListener;
	private Handler mMsgListItemHandler;
	private String mHighlight;
	private Context mContext;
	private boolean mAutoRequery;

	static final int READY = 0;
	static final int LOADING = 1;
	static final int LOADED = 2;
	private int mLoading = READY;
	public String recipient;
	public final TMmsSmsCursor mTCursor;

	public MessageListAdapter(Context context, Cursor c, ListView listView,
			boolean useDefaultColumnsMap, String highlight, boolean autoRequery) {
		super(context, c, autoRequery /* auto-requery */);
		mHighlight = highlight != null ? highlight.toLowerCase() : null;

		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mListView = listView;
		mMessageItemCache = new LinkedHashMap<Long, MessageItem>(10, 1.0f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry eldest) {
				return size() > CACHE_SIZE;
			}
		};

		if (useDefaultColumnsMap) {
			mColumnsMap = new ColumnsMap();
		} else {
			mColumnsMap = new ColumnsMap(c);
		}

		mTCursor = new TMmsSmsCursor(context, new String[] {});
		mContext = context;
		mAutoRequery = autoRequery;
	}

	public void setAddress(String address) {
		Log.d("Mms", "set address " + address);
		recipient = address;
		if ((this.getCursor() == null || this.getCursor().getCount() == 0)
				&& recipient != null && !recipient.equals("")) {
			preloadRemoteSMS();
		}
		if (recipient == null) {
			mLoading = LOADED;
		}

	}

	public class UpdateListHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.d("Mms", "update list handler");
			onRefeshContent();
			((ComposeMessageActivity) mContext).getListView().invalidate();
		}

	}

	private void onRefeshContent() {
		this.notifyDataSetChanged();
	}

	public final UpdateListHandler handler = new UpdateListHandler();

	public void preloadRemoteSMS() {
		mTCursor.clearAllSms(mContext.getContentResolver());
		// currentPage++;
		// new Timer().schedule(new RetriveAchivedSMSTimerTask(5, recipient,
		// currentPage), 0);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		if (view instanceof MessageListItem) {
			String type = cursor.getString(mColumnsMap.mColumnMsgType);
			long msgId = cursor.getLong(mColumnsMap.mColumnMsgId);

			MessageItem msgItem = getCachedMessageItem(type, msgId, cursor);
			if (msgItem != null) {
				((MessageListItem) view).bind(msgItem);
				((MessageListItem) view)
						.setMsgListItemHandler(mMsgListItemHandler);
			}
		}
	}

	
	public void retriveMoreAction()
	{
		mTCursor.retrieveArchivedSMS(recipient, handler);
	}
	/*
	private int getRealPosition(int position) {
		switch (mLoading) {
		case READY:
		case LOADING:
			return position - 1;
		case LOADED:
		default:
			return position;
		}
	}
	
	
	@Override
	public int getCount() {
		switch (this.mLoading) {
		case READY:
		case LOADING:
			return super.getCount() + 1;
		case LOADED:
			return super.getCount();
		}
		return super.getCount();
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		try {
			if (!mDataValid) {
				throw new IllegalStateException(
						"this should only be called when the cursor is valid");
			}
			if (!mCursor.moveToPosition(getRealPosition(position))) {
				throw new IllegalStateException(
						"couldn't move cursor to position " + position);
			}
			View v;
			if (convertView == null) {
				v = newView(mContext, mCursor, parent);
			} else {
				if (convertView instanceof Button) {
					v = newView(mContext, mCursor, parent);
				} else {
					v = convertView;
				}
			}
			bindView(v, mContext, mCursor);
			return v;

		} catch (Exception e) {
			if (mLoading == READY || mLoading == LOADING) {
				if (position == getCount()-1) {
					Button b = new Button(mContext);
					b.setText("More...");
					b.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							mLoading = LOADING;
							handler.sendEmptyMessage(0);
							mTCursor.retrieveArchivedSMS(recipient, handler);
						}
					});
					return b;
				} else {
					return super.getView(position, convertView, parent);
				}
			} else {
				return super.getView(position, convertView, parent);
			}
		}
	}*/



	public interface OnDataSetChangedListener {
		void onDataSetChanged(MessageListAdapter adapter);

		void onContentChanged(MessageListAdapter adapter);
	}

	public void setOnDataSetChangedListener(OnDataSetChangedListener l) {
		mOnDataSetChangedListener = l;
	}

	public void setMsgListItemHandler(Handler handler) {
		mMsgListItemHandler = handler;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		if (LOCAL_LOGV) {
			Log.v(TAG, "MessageListAdapter.notifyDataSetChanged().");
		}

		mListView.setSelection(mListView.getCount());
		mMessageItemCache.clear();

		if (mOnDataSetChangedListener != null) {
			mOnDataSetChangedListener.onDataSetChanged(this);
		}
	}

	@Override
	protected void onContentChanged() {
		if (mAutoRequery) {
			super.onContentChanged();
			return;
		}
		if (this.getCursor() != null && !this.getCursor().isClosed()) {
			if (mOnDataSetChangedListener != null) {
				mOnDataSetChangedListener.onContentChanged(this);
			}
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mInflater.inflate(R.layout.message_list_item, parent, false);
	}

	public MessageItem getCachedMessageItem(String type, long msgId, Cursor c) {
		MessageItem item = mMessageItemCache.get(getKey(type, msgId));
		if (item == null) {
			try {
				item = new MessageItem(mContext, type, c, mColumnsMap,
						mHighlight);
				mMessageItemCache.put(getKey(item.mType, item.mMsgId), item);
			} catch (MmsException e) {
				Log.e(TAG, e.getMessage());
			}
		}
		return item;
	}

	private static long getKey(String type, long id) {
		if (type.equals("mms")) {
			return -id;
		} else {
			return id;
		}
	}

	public static class ColumnsMap {
		public int mColumnMsgType;
		public int mColumnMsgId;
		public int mColumnMsgGuid;
		public int mColumnMsgSyncState;
		public int mColumnSmsAddress;
		public int mColumnSmsBody;
		public int mColumnSmsDate;
		public int mColumnSmsRead;
		public int mColumnSmsType;
		public int mColumnSmsStatus;
		public int mColumnSmsLocked;
		public int mColumnMmsSubject;
		public int mColumnMmsSubjectCharset;
		public int mColumnMmsDate;
		public int mColumnMmsRead;
		public int mColumnMmsMessageType;
		public int mColumnMmsMessageBox;
		public int mColumnMmsDeliveryReport;
		public int mColumnMmsReadReport;
		public int mColumnMmsErrorType;
		public int mColumnMmsLocked;

		public ColumnsMap() {
			mColumnMsgType = COLUMN_MSG_TYPE;
			mColumnMsgId = COLUMN_ID;
			mColumnMsgGuid = COLUMN_GUID;
			mColumnMsgSyncState = COLUMN_SYNC_STATE;
			mColumnSmsAddress = COLUMN_SMS_ADDRESS;
			mColumnSmsBody = COLUMN_SMS_BODY;
			mColumnSmsDate = COLUMN_SMS_DATE;
			mColumnSmsType = COLUMN_SMS_TYPE;
			mColumnSmsStatus = COLUMN_SMS_STATUS;
			mColumnSmsLocked = COLUMN_SMS_LOCKED;
			mColumnMmsSubject = COLUMN_MMS_SUBJECT;
			mColumnMmsSubjectCharset = COLUMN_MMS_SUBJECT_CHARSET;
			mColumnMmsMessageType = COLUMN_MMS_MESSAGE_TYPE;
			mColumnMmsMessageBox = COLUMN_MMS_MESSAGE_BOX;
			mColumnMmsDeliveryReport = COLUMN_MMS_DELIVERY_REPORT;
			mColumnMmsReadReport = COLUMN_MMS_READ_REPORT;
			mColumnMmsErrorType = COLUMN_MMS_ERROR_TYPE;
			mColumnMmsLocked = COLUMN_MMS_LOCKED;
		}

		public ColumnsMap(Cursor cursor) {
			// Ignore all 'not found' exceptions since the custom columns
			// may be just a subset of the default columns.
			try {
				mColumnMsgType = cursor
						.getColumnIndexOrThrow(MmsSms.TYPE_DISCRIMINATOR_COLUMN);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnMsgId = cursor.getColumnIndexOrThrow(BaseColumns._ID);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnSmsAddress = cursor.getColumnIndexOrThrow(Sms.ADDRESS);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnSmsBody = cursor.getColumnIndexOrThrow(Sms.BODY);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnSmsDate = cursor.getColumnIndexOrThrow(Sms.DATE);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnSmsType = cursor.getColumnIndexOrThrow(Sms.TYPE);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnSmsStatus = cursor.getColumnIndexOrThrow(Sms.STATUS);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnSmsLocked = cursor.getColumnIndexOrThrow(Sms.LOCKED);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnMmsSubject = cursor.getColumnIndexOrThrow(Mms.SUBJECT);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnMmsSubjectCharset = cursor
						.getColumnIndexOrThrow(Mms.SUBJECT_CHARSET);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnMmsMessageType = cursor
						.getColumnIndexOrThrow(Mms.MESSAGE_TYPE);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnMmsMessageBox = cursor
						.getColumnIndexOrThrow(Mms.MESSAGE_BOX);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnMmsDeliveryReport = cursor
						.getColumnIndexOrThrow(Mms.DELIVERY_REPORT);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnMmsReadReport = cursor
						.getColumnIndexOrThrow(Mms.READ_REPORT);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnMmsErrorType = cursor
						.getColumnIndexOrThrow(PendingMessages.ERROR_TYPE);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}

			try {
				mColumnMmsLocked = cursor.getColumnIndexOrThrow(Mms.LOCKED);
			} catch (IllegalArgumentException e) {
				Log.w("colsMap", e.getMessage());
			}
		}
	}
}
