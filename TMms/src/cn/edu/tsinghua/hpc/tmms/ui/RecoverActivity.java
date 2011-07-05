package cn.edu.tsinghua.hpc.tmms.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.ClientProtocolException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Threads;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;
import cn.edu.tsinghua.hpc.syncbroker.ElementNotFound;
import cn.edu.tsinghua.hpc.syncbroker.SMSRecord;
import cn.edu.tsinghua.hpc.syncbroker.SyncRecord;
import cn.edu.tsinghua.hpc.tmms.R;
import cn.edu.tsinghua.hpc.tmms.service.TMessageSyncHelper;
import cn.edu.tsinghua.hpc.tmms.syncaction.MmsUtils;
import cn.edu.tsinghua.hpc.tmms.syncaction.SyncAction;
import cn.edu.tsinghua.hpc.tmms.syncaction.SyncState;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMmsSms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TSms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TThreads;

public class RecoverActivity extends ListActivity {
	
	private static final String TAG = "RecoverActivity";

	private RecoverActivityListAdapter mAdapter;
	private DeleteMessageQueryHandler mQueryHandler;
	private Uri conversationUri;
	private Button mRecoverThread = null;
	private Button mFinalDeleteThread = null;
	private View.OnClickListener mRecover;
	private View.OnClickListener mFinalDelete;
	private ListView mListView = null;


	private static final int MENU_SELECT_ALL = 0;
	private static final int MENU_DESELECT_ALL = 1;
	/*
	private static final int MENU_OK = 2;
	private static final int MENU_REMOVE = 3;
	private static final int MENU_EMPTY = 4;
	private static final int MENU_CANCEL = 5;
	*/
	private static final int DIALOG_FINAL_DELETE_ID = 0;
	private static final int TOKEN_DELETED_MESSEAGES = 1901;
	private static final int TOKEN_UNDELET_MESSEAGES = 2001;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recover_thread_screen);

		mRecoverThread = (Button)findViewById(R.id.btn_recover_thread);
		mFinalDeleteThread = (Button)findViewById(R.id.btn_final_delete_thread);
		mRecover = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(TAG,"mRecover+++++++");
				Button button = (Button) v;
				switch (button.getId()) { 
					case R.id.btn_recover_thread: {	// 
						recover();
						break;
					}
					default:    
						break;    
				}    
			}
		};
		
		mFinalDelete = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				Button button = (Button) v;
				switch (button.getId()) { 
					case R.id.btn_final_delete_thread: {	// 
						finalDelete();
						break;
					}
					default:    
						break;    
				}    
			}
		};
		
		mRecoverThread.setOnClickListener(mRecover);
		mFinalDeleteThread.setOnClickListener(mFinalDelete);

		mQueryHandler = new DeleteMessageQueryHandler(getContentResolver());
//		conversationUri = Uri.parse(getIntent().getStringExtra(
//				"conversationUri"));

		if(getIntent().getStringExtra("conversationUri").equals("newThreads")){
			conversationUri = null;
		}else{
			conversationUri = Uri.parse(getIntent().getStringExtra(
					"conversationUri"));
		}
		mListView = getListView();
		mListView.setItemsCanFocus(false);
		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		initListAdapter();
	}

	private void initListAdapter() {
		mAdapter = new RecoverActivityListAdapter(this, null, true);
		mAdapter.setOnContentChangedListener(mContentChangedListener);
		setListAdapter(mAdapter);
	}

	private final RecoverActivityListAdapter.OnContentChangedListener mContentChangedListener = new RecoverActivityListAdapter.OnContentChangedListener() {
		public void onContentChanged(RecoverActivityListAdapter adapter) {
			startAsyncQuery();
		}
	};

	private void recover() {
		boolean hasChecked = false;
		SparseBooleanArray array = mListView.getCheckedItemPositions();
		Log.i(TAG,"+++"+array.size());
		for (int i = 0; i < array.size(); i++) {
			if (array.valueAt(i)) {
				hasChecked = true;
				break;
			}
		}
		if (hasChecked) {
			doRecover();
		}
	}
	
	private void doRecover() {
		SparseBooleanArray array = mListView.getCheckedItemPositions();
		Cursor cursor = this.mAdapter.getCursor();
		if (cursor != null || array.size() > 0) {
			for (int i = 0; i < array.size(); i++) {
				if (cursor.moveToPosition(array.keyAt(i)) && array.valueAt(i)) {
					long itemId = cursor.getLong(cursor
							.getColumnIndex(BaseColumns._ID));
					long guid = cursor.getLong(cursor
							.getColumnIndex("guid"));
					String type = cursor
							.getString(cursor
									.getColumnIndex(MmsSms.TYPE_DISCRIMINATOR_COLUMN));
					Uri uri = null;
					if ("sms".equals(type)) {
						uri = ContentUris.withAppendedId(TSms.CONTENT_URI,
								itemId);
					} else if ("mms".equals(type)) {
						uri = ContentUris.withAppendedId(TMms.CONTENT_URI,
								itemId);
					}
//					Log.v(TAG,"uri========="+ uri);
					if (uri != null) {
						ContentValues values = new ContentValues();
						values.put("sync_state",
								SyncState.SYNC_STATE_RECOVER);
						mQueryHandler.startUpdate(TOKEN_UNDELET_MESSEAGES,
								null, uri, values, null, null);
					}
				}
			}
		}
		cursor.close();
		finish();
		
	}
	
	private void finalDelete() {
//		Log.i(TAG,"finalDelete+++++++");
		boolean hasChecked = false;
		SparseBooleanArray array = mListView.getCheckedItemPositions();
		for (int i = 0; i < array.size(); i++) {
			if (array.valueAt(i)) {
				hasChecked = true;
				break;
			}
		}
		if (hasChecked) {
			showDialog(DIALOG_FINAL_DELETE_ID);
		}
	}
	
	private void doFinalDelete() {
		SparseBooleanArray array = mListView.getCheckedItemPositions();
		Cursor cursor = this.mAdapter.getCursor();
		if (cursor != null || array.size() > 0) {
			for (int i = 0; i < array.size(); i++) {
				if (cursor.moveToPosition(array.keyAt(i)) && array.valueAt(i)) {
					long itemId = cursor.getLong(cursor
							.getColumnIndex(BaseColumns._ID));
					long guid = cursor.getLong(cursor
							.getColumnIndex("guid"));
					String type = cursor
							.getString(cursor
									.getColumnIndex(MmsSms.TYPE_DISCRIMINATOR_COLUMN));
					Uri uri = null;
					if ("sms".equals(type)) {
						uri = ContentUris.withAppendedId(TSms.CONTENT_URI,
								itemId);
					} else if ("mms".equals(type)) {
						uri = ContentUris.withAppendedId(TMms.CONTENT_URI,
								itemId);
					}
					Log.v(TAG,"uri========="+ uri);
					if (uri != null) {
						ContentValues values = new ContentValues();
						values.put("sync_state",
								SyncState.SYNC_STATE_REMOVED);
						mQueryHandler.startUpdate(TOKEN_DELETED_MESSEAGES,
								null, uri, values, null, null);
					}
				}
			}
		}
		
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		menu.add(0, MENU_SELECT_ALL, 0, R.string.select_all);
		menu.add(0, MENU_DESELECT_ALL, 0, R.string.unselect_all);
		/*
		menu.add(0, MENU_OK, 0, android.R.string.ok);
		menu.add(0, MENU_REMOVE, 0, R.string.remove);
		menu.add(0, MENU_EMPTY, 0, R.string.empty);
		menu.add(0, MENU_CANCEL, 0, android.R.string.cancel);
		*/
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final ListView listView = getListView();
			//add by chenqiang
		switch (item.getItemId()) {
		case MENU_SELECT_ALL:
			for (int i = 0; i < listView.getCount(); i++) {
				listView.setItemChecked(i, true);
			}
			break;
			//add by chenqiang
//			for (int i = 0; i < RecoverActivityListAdapter.cblist.size(); i++) {
//							
//				RecoverActivityListAdapter.cblist.get(i).setChecked(true);
//			}
//			break;
		case MENU_DESELECT_ALL:
			for (int i = 0; i < listView.getCount(); i++) {
//				listView.setItemChecked(i, false);
				listView.setItemChecked(i, !listView.isItemChecked(i));
			}
			break;
			//add by chenqiang
//			for (int i = 0; i < RecoverActivityListAdapter.cblist.size(); i++) {
//				RecoverActivityListAdapter.cblist.get(i).setChecked(!RecoverActivityListAdapter.cblist.get(i).isChecked());
//			}
//			break;
			/*		
		case MENU_EMPTY:{
			Cursor cursor = this.mAdapter.getCursor();
			if (cursor != null && cursor.moveToFirst()) {
				do {
					long itemId = cursor.getLong(cursor
							.getColumnIndex(BaseColumns._ID));
					long guid = cursor.getLong(cursor
							.getColumnIndex("guid"));
					String type = cursor
							.getString(cursor
									.getColumnIndex(MmsSms.TYPE_DISCRIMINATOR_COLUMN));
					Uri uri = null;
					if ("sms".equals(type)) {
						uri = ContentUris.withAppendedId(TSms.CONTENT_URI,
								itemId);
					} else if ("mms".equals(type)) {
						uri = ContentUris.withAppendedId(TMms.CONTENT_URI,
								itemId);
					}
					if (uri != null) {
						ContentValues values = new ContentValues();
						values.put("sync_state",
								SyncState.SYNC_STATE_REMOVED);
						mQueryHandler.startUpdate(TOKEN_UNDELET_MESSEAGES,
								null, uri, values, null, null);
					}
				} while(cursor.moveToNext());
			}
			break;
		}		
		case MENU_CANCEL:
			// FIXME: delete all the synced contact
			mQueryHandler.startDelete(TOKEN_DELETED_MESSEAGES, null,
					conversationUri, "sync_state = '"
							+ SyncState.SYNC_STATE_REMOTE_DELETE + "'", null);
			finish();
			break;
					*/
		}
		return true;
	}

	@Override
	protected void onStart() {
//		Log.i(TAG, "------onstart-------");
//		//add by chenqiang
//		RecoverActivityListAdapter.cblist.clear();
		super.onStart();
		if (TMessageSyncHelper.tryLock(this)) {
			
//			startRetriveDeleteMessages(); //todo：不用每次都请求网络服务器中的数据吧？？？？
			
			startAsyncQuery();
		}else{
			Toast.makeText(this, "wait util sync service", 1000);
			finish();
		}
	}

	@Override
	protected void onStop() {
		//todo：退出回收站，不应该删除数据吧。
//		TMessageSyncHelper.releaseLock(this);
//		new Timer().schedule(new TimerTask() {
//
//			@Override
//			public void run() {
//				getContentResolver().delete(
//						TThreads.CONTENT_URI,
//						"sync_state = '" + SyncState.SYNC_STATE_REMOTE_DELETE
//								+ "'", null);
//			}
//		}, 0);
		super.onStop();
	}

	private void startRetriveDeleteMessages() {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				int page = 0;
				List<SyncRecord> result = new ArrayList<SyncRecord>();
				String address = MessageUtils.getAddressByThreadId(
						RecoverActivity.this, ContentUris
								.parseId(conversationUri));
				do {
					page++;
					try {
						result = SyncAction.recoverMessage(
								RecoverActivity.this, page, address);
						for (SyncRecord i : result) {
							SMSRecord s = (SMSRecord) i;
							MmsUtils.insertOneMessageInto(RecoverActivity.this,
									s, SyncState.SYNC_STATE_REMOTE_DELETE);
						}
					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (result.size() > 0);

			}
		}, 0);
	}

	private void startAsyncQuery() {
		try {
			if(conversationUri!=null){
			setTitle(getString(R.string.refreshing));
			setProgressBarIndeterminateVisibility(true);
			mQueryHandler.startQuery(TOKEN_DELETED_MESSEAGES, null,
					conversationUri, MessageListAdapter.PROJECTION,
					"( sync_state = '" + SyncState.SYNC_STATE_DELETED
							+ "' OR sync_state = '"
							+ SyncState.SYNC_STATE_REMOTE_DELETE 
					+ "' )",
							null,"date ASC");
			Log.d(TAG, conversationUri.toString());
			}
		} catch (SQLiteException e) {

		}
	}

	private final class DeleteMessageQueryHandler extends AsyncQueryHandler {

		public DeleteMessageQueryHandler(ContentResolver contentResolver) {
			super(contentResolver);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			switch (token) {
			case TOKEN_DELETED_MESSEAGES:
				//add by chenqiang
				if(cursor != null){
					Log.d(TAG, "cursor count is " + cursor.getCount());
					mAdapter.changeCursor(cursor);
				}
				setProgressBarIndeterminateVisibility(false);
				setTitle(getString(R.string.app_label));
				break;
			default:
				break;
			}
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_FINAL_DELETE_ID: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.final_delete_message_title);
			builder.setMessage(R.string.final_delete_message_message);
			builder.setCancelable(false);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							doFinalDelete();
						}
					});
			builder.setNegativeButton(android.R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int which) {
							dialog.dismiss();
						}
					});	
			AlertDialog dialog = builder.create();
			return dialog;
		}
		}
		return null;
	}	
}
