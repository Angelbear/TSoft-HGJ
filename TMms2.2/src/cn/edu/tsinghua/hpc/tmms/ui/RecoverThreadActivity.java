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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.provider.Telephony.Sms.Conversations;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import cn.edu.tsinghua.hpc.syncbroker.ElementNotFound;
import cn.edu.tsinghua.hpc.syncbroker.SMSRecord;
import cn.edu.tsinghua.hpc.syncbroker.SyncRecord;
import cn.edu.tsinghua.hpc.tmms.MmsConfig;
import cn.edu.tsinghua.hpc.tmms.R;
import cn.edu.tsinghua.hpc.tmms.provider.MmsSmsDatabaseHelper;
import cn.edu.tsinghua.hpc.tmms.service.TMessageSyncHelper;
import cn.edu.tsinghua.hpc.tmms.syncaction.MmsUtils;
import cn.edu.tsinghua.hpc.tmms.syncaction.SyncAction;
import cn.edu.tsinghua.hpc.tmms.syncaction.SyncState;
import cn.edu.tsinghua.hpc.tmms.util.TIntent;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMmsSms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TSms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TThreads;

public class RecoverThreadActivity extends ListActivity {
	
	private final String TAG = "RecoverThreadActivity";

	private DeletedThreadsQueryHandler mQueryHandler;
	private RecoverThreadActivityListAdapter mAdapter;
	private Button mRecoverThread = null;
	private Button mFinalDeleteThread = null;
	private View.OnClickListener mRecover;
	private View.OnClickListener mFinalDelete;
	private ListView mListView = null;
	
	private static final int MENU_SELECT_ALL = 0;
	private static final int MENU_DESELECT_ALL = 1;
	/*
	private static final int MENU_RECOVER = 2;
	private static final int MENU_REMOVE = 3;
	private static final int MENU_EMPTY = 4;
	private static final int MENU_CANCEL = 5;
    */
	private static final int TOKEN_DELETED_THREADS = 2901;
	private static final int TOKEN_UNDELET_THREADS = 3001;
//	private static final int TOKEN_DELETED_MESSEAGES = 1901;
//	private static final int TOKEN_UNDELET_MESSEAGES = 2001;

	private static final int BUSY_DIALOG = 0;
	private static final int FINAL_DELETE_DIALOG = 1;
	private static final int RECOVER_FINISH = 0;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recover_thread_screen);
		mRecoverThread = (Button)findViewById(R.id.btn_recover_thread);
		mFinalDeleteThread = (Button)findViewById(R.id.btn_final_delete_thread);
		mRecover = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
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
		
		mQueryHandler = new DeletedThreadsQueryHandler(getContentResolver());

		mListView = getListView();
		
		mListView.setItemsCanFocus(false);
		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		
		initListAdapter();
	}

	private void initListAdapter() {
		mAdapter = new RecoverThreadActivityListAdapter(this, null, false);
		mAdapter.setOnContentChangedListener(mContentChangedListener);
		setListAdapter(mAdapter);
	}

	private final RecoverThreadActivityListAdapter.OnContentChangedListener mContentChangedListener = new RecoverThreadActivityListAdapter.OnContentChangedListener() {
		public void onContentChanged(RecoverThreadActivityListAdapter adapter) {
			startAsyncQuery();
		}
	};

	private void recover() {
		boolean hasChecked = false;
		SparseBooleanArray array = mListView.getCheckedItemPositions();
		Log.d(TAG, array.size()+"----------");
		for (int i = 0; i < array.size(); i++) {
			if (array.valueAt(i)) {
				hasChecked = true;
				break;
			}
		}
		if (hasChecked) {
			showDialog(BUSY_DIALOG);
			doRecover();
//			recoverTest();
		}
	}
	
	private void doRecover() {
//		new Timer().schedule(new TimerTask() {
//			@Override
//			public void run() {
//				SparseBooleanArray array = mListView.getCheckedItemPositions();
//				Cursor cursor = mAdapter.getCursor();
//				if (cursor != null || array.size() > 0) {
//					for (int i = 0; i < array.size(); i++) {
//						if (cursor.moveToPosition(array.keyAt(i))&& array.valueAt(i)) {
//							long itemId = cursor.getLong(cursor.getColumnIndex(Threads._ID));
//							Uri uri = ContentUris.withAppendedId(TThreads.CONTENT_URI, itemId);
//							if (uri != null) {
//								ContentValues values = new ContentValues();
//								values.put("sync_state", SyncState.SYNC_STATE_RECOVER);
//								mQueryHandler.startUpdate(TOKEN_UNDELET_THREADS, null, uri, values, null, null);
//								MmsUtils.markMessageOrThread(
//										RecoverThreadActivity.this, 
//										ContentUris.withAppendedId(Uri.withAppendedPath(TMmsSms.AUTHORITY_URI, "threads"), itemId),
//										SyncState.SYNC_STATE_PRESENT);
//							}
//						}
//					}
//				}
//				handler.sendEmptyMessage(RECOVER_FINISH);
				
				//add by chenqiang
				SparseBooleanArray array = mListView.getCheckedItemPositions();
				Cursor cursor = mAdapter.getCursor();
				if (cursor != null || array.size() > 0) {
					for (int i = 0; i < array.size(); i++) {
						if (cursor.moveToPosition(array.keyAt(i)) && array.valueAt(i)) {
							long threadId = cursor.getLong(cursor.getColumnIndex(TSms.THREAD_ID));
							long msgID = cursor.getLong(cursor
									.getColumnIndex(BaseColumns._ID));
							Log.v(TAG,"threadId========="+ threadId);
							Log.v(TAG,"msgID========="+ msgID);
							long guid = cursor.getLong(cursor
									.getColumnIndex("guid"));
							String type = cursor
									.getString(cursor
											.getColumnIndex(MmsSms.TYPE_DISCRIMINATOR_COLUMN));
							Uri uri = null;
							if ("sms".equals(type)) {
								uri = ContentUris.withAppendedId(TSms.CONTENT_URI,
										msgID);
							} else if ("mms".equals(type)) {
								uri = ContentUris.withAppendedId(TMms.CONTENT_URI,
										msgID);
							}
							Log.v(TAG,"uri========="+ uri);
							if (uri != null) {
								ContentValues values = new ContentValues();
								values.put("sync_state",
										SyncState.SYNC_STATE_RECOVER);
								mQueryHandler.startUpdate(TOKEN_UNDELET_THREADS,
										null, uri, values, null, null);
								Uri threadUri = ContentUris.withAppendedId(Uri.withAppendedPath(TMmsSms.AUTHORITY_URI, "threads"), threadId);
								Log.v(TAG,"threadUri========="+ threadUri);
								MmsUtils.markMessageOrThread(
									RecoverThreadActivity.this, threadUri,SyncState.SYNC_STATE_PRESENT);
							}
						}
					}
				}
				handler.sendEmptyMessage(RECOVER_FINISH);
				cursor.close();
			}
//		}, 0);
		
//	}
	
	private void finalDelete() {
		boolean hasChecked = false;
		SparseBooleanArray array = mListView.getCheckedItemPositions();
		for (int i = 0; i < array.size(); i++) {
			if (array.valueAt(i)) {
				hasChecked = true;
				break;
			}
		}
		if (hasChecked) {
			showDialog(FINAL_DELETE_DIALOG);
		}
	}
	
	private void doFinalDelete() {
//		new Timer().schedule(new TimerTask() {
//			@Override
//			public void run() {
//				SparseBooleanArray array = mListView.getCheckedItemPositions();
//				Cursor cursor = mAdapter.getCursor();
//				if (cursor != null || array.size() > 0) {
//					for (int i = 0; i < array.size(); i++) {
//						if (cursor.moveToPosition(array.keyAt(i)) && array.valueAt(i)) {
//							long itemId = cursor.getLong(cursor.getColumnIndex(Threads._ID));
//							Uri uri = ContentUris.withAppendedId(TThreads.CONTENT_URI, itemId);
//							if (uri != null) {
//								ContentValues values = new ContentValues();
//								values.put("sync_state", SyncState.SYNC_STATE_REMOVED);
//								mQueryHandler.startUpdate(TOKEN_UNDELET_THREADS, null, uri,values, null, null);
//								MmsUtils.markMessageOrThread(
//								    RecoverThreadActivity.this,
//									ContentUris.withAppendedId(Uri.withAppendedPath(TMmsSms.AUTHORITY_URI,"threads"),itemId),
//									SyncState.SYNC_STATE_REMOVED);
//							}
//						}
//					}
//				}
				//add by chenqiang
				SparseBooleanArray array = mListView.getCheckedItemPositions();
				Cursor cursor = mAdapter.getCursor();
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
								mQueryHandler.startUpdate(TOKEN_UNDELET_THREADS,
										null, uri, values, null, null);
							}
						}
					}
				}
//			}
//		}, 0);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		menu.add(0, MENU_SELECT_ALL, 0, R.string.select_all);
		menu.add(0, MENU_DESELECT_ALL, 0, R.string.unselect_all);
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
//			for (int i = 0; i < RecoverThreadActivityListAdapter.cblist.size(); i++) {
//				RecoverThreadActivityListAdapter.cblist.get(i).setChecked(true);
//			}
//			break;
		case MENU_DESELECT_ALL:
			for (int i = 0; i < listView.getCount(); i++) {
//				listView.setItemChecked(i, false);
				listView.setItemChecked(i, !listView.isItemChecked(i));
			}
			break;
//			for (int i = 0; i < RecoverThreadActivityListAdapter.cblist.size(); i++) {
//				RecoverThreadActivityListAdapter.cblist.get(i).setChecked(!RecoverThreadActivityListAdapter.cblist.get(i).isChecked());
//			}
//			break;
		/*
		case MENU_EMPTY: {
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					Cursor cursor = mAdapter.getCursor();
					if (cursor != null && cursor.moveToFirst()) {
						do {
								long itemId = cursor.getLong(cursor
										.getColumnIndex(Threads._ID));
								Uri uri = ContentUris.withAppendedId(
										TThreads.CONTENT_URI, itemId);
								if (uri != null) {
									ContentValues values = new ContentValues();
									values.put("sync_state",
											SyncState.SYNC_STATE_REMOVED);
									mQueryHandler.startUpdate(
											TOKEN_UNDELET_THREADS, null, uri,
											values, null, null);
									MmsUtils.markMessageOrThread(
											RecoverThreadActivity.this,
											ContentUris.withAppendedId(
												Uri.withAppendedPath(
													TMmsSms.AUTHORITY_URI,
													"threads"),
									            // Uri.parse("content://mms-sms/threads"),
												itemId),
											SyncState.SYNC_STATE_REMOVED);
								}
						} while(cursor.moveToNext());
					}
				}
			}, 0);
			break;
		}	
		*/	
		}
		return true;
	}

	private DialogShowHandler handler = new DialogShowHandler();

	private class DialogShowHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			dismissDialog(BUSY_DIALOG);
			switch (msg.what) {
			case RECOVER_FINISH:
				Toast.makeText(RecoverThreadActivity.this, "Recover OK", 1000)
						.show();
				finish();
				break;
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case BUSY_DIALOG: {
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage("Please wait while recovering...");
			dialog.setIndeterminate(true);
			dialog.setCancelable(false);
			return dialog;
		}
		case FINAL_DELETE_DIALOG: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.final_delete_thread_title);
			builder.setMessage(R.string.final_delete_thread_message);
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

	private void startRetriveDeleteThreads() {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				int page = 0;
				List<SyncRecord> result = new ArrayList<SyncRecord>();
				do {
					page++;
					try {
//						result = SyncAction.retriveDeletedThread(
//								RecoverThreadActivity.this, page, 5);
						//add by chenqiang
						result = SyncAction.recoverMessage(RecoverThreadActivity.this, page, null);
						for (SyncRecord i : result) {
							SMSRecord s = (SMSRecord) i;
							MmsUtils.inserRemoteThreadInto(
									RecoverThreadActivity.this, s,
									SyncState.SYNC_STATE_REMOTE_DELETE);
						}
					} catch (ClientProtocolException e) {
					} catch (ElementNotFound e) {
					} catch (IOException e) {
					}
				} while (result.size() > 0);

			}
		}, 0);
	}

	@Override
	protected void onStart() {
		super.onStart();
//		//add by chenqiang
//		RecoverThreadActivityListAdapter.cblist.clear();
		
//		if (TMessageSyncHelper.lock(this)) {
//			startRetriveDeleteThreads();
			startAsyncQuery();
//		} else {
//			finish();
//		}
	}

	@Override
	protected void onStop() {
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

	private void startAsyncQuery() {
		try {
			setTitle(getString(R.string.refreshing));
			setProgressBarIndeterminateVisibility(true);
			mQueryHandler.startQuery(TOKEN_DELETED_THREADS, null,
					Uri.withAppendedPath(TMmsSms.AUTHORITY_URI, "complete-conversations"),
//					TSms.CONTENT_URI
					// Uri.parse("content://mms-sms/all-threads"),
//					new String[] { Threads._ID, "sync_state" },
					MessageListAdapter.PROJECTION,
					"sync_state = '" + SyncState.SYNC_STATE_DELETED
							+ "' OR sync_state = '"
							+ SyncState.SYNC_STATE_REMOTE_DELETE + "'", null,
					null);

		} catch (SQLiteException e) {

		}
	}

	private final class DeletedThreadsQueryHandler extends AsyncQueryHandler {

		public DeletedThreadsQueryHandler(ContentResolver contentResolver) {
			super(contentResolver);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			mAdapter.changeCursor(cursor);
			switch (token) {
			case TOKEN_DELETED_THREADS:
				// Log.d("Mms", "cursor count is " + cursor.getCount());
				mAdapter.changeCursor(cursor);
				setProgressBarIndeterminateVisibility(false);
				setTitle(getString(R.string.app_label));
				break;
			default:
				break;
			}
		}
	}

}
