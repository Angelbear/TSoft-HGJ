package cn.edu.tsinghua.hpc.tcontacts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.ClientProtocolException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.pim.vcard.exception.VCardException;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import cn.edu.tsinghua.hpc.syncbroker.ElementNotFound;
import cn.edu.tsinghua.hpc.syncbroker.SyncRecord;
import cn.edu.tsinghua.hpc.tcontacts.pim.ContactStruct;
import cn.edu.tsinghua.hpc.tcontacts.syncaction.ContactsUtility;
import cn.edu.tsinghua.hpc.tcontacts.syncaction.SyncAction;
import cn.edu.tsinghua.hpc.tcontacts.syncaction.SyncState;
import cn.edu.tsinghua.hpc.tcontacts.util.TContactsContract.TContacts;
import cn.edu.tsinghua.hpc.tcontacts.util.TContactsContract.TRawContacts;

public class RecoverActivity extends Activity implements OnClickListener{

	private RecoverActivityListAdapter mAdapter;
	private DeleteContactsQueryHandler mQueryHandler;

	private static final int MENU_SELECT_ALL = 0;
	private static final int MENU_DESELECT_ALL = 1;
	private static final int MENU_RECOVER = 2;
	private static final int MENU_CLEAR = 3;
	private static final int MENU_CANCEL = 4;
	

	private static final int TOKEN_DELETED_CONTACTS = 2901;
	private static final int TOKEN_FRESH_CONTACTS = 3001;

	static final String[] PROJECTION = new String[] { Contacts._ID,
			Contacts.DISPLAY_NAME, "guid", "sync_state" };
	
	ListView listView;
	Button recoverBtn;
	Button removeBtn;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recover_layout_main);
		mQueryHandler = new DeleteContactsQueryHandler(getContentResolver());
		//final ListView listView = getListView();
	    listView=(ListView) findViewById(R.id.recover_list);
		listView.setItemsCanFocus(false);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		recoverBtn = (Button)findViewById(R.id.btn_recover);
		recoverBtn.setOnClickListener(this);
		removeBtn = (Button)findViewById(R.id.btn_remove);
		removeBtn.setOnClickListener(this);
		
		initListAdapter();
	}

	private void initListAdapter() {
		mAdapter = new RecoverActivityListAdapter(this, null, true);
		mAdapter.setOnContentChangedListener(mContentChangedListener);
		listView.setAdapter(mAdapter) ;
	}

	private final RecoverActivityListAdapter.OnContentChangedListener mContentChangedListener = new RecoverActivityListAdapter.OnContentChangedListener() {
		public void onContentChanged(RecoverActivityListAdapter adapter) {
			startAsyncQuery();
		}
	};

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		menu.add(0, MENU_SELECT_ALL, 0, R.string.select_all);
		menu.add(0, MENU_DESELECT_ALL, 0, R.string.unselect_all);
		//menu.add(0, MENU_RECOVER, 0, R.string.menu_recover);
		//menu.add(0, MENU_CLEAR, 0, R.string.final_delete);
		menu.add(0, MENU_CANCEL, 0, android.R.string.cancel);
//		return true;
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//final ListView listView = getListView();

		switch (item.getItemId()) {
		case MENU_SELECT_ALL:
			for (int i = 0; i < listView.getCount(); i++) {
				listView.setItemChecked(i, true);
			}
			break;
		case MENU_DESELECT_ALL:
			for (int i = 0; i < listView.getCount(); i++) {
				listView.setItemChecked(i, false);
			}
			break;
		case MENU_RECOVER: {
			SparseBooleanArray array = listView.getCheckedItemPositions();
			Cursor cursor = this.mAdapter.getCursor();
			if (cursor != null || array.size() > 0) {
				for (int i = 0; i < array.size(); i++) {
					if (cursor.moveToPosition(array.keyAt(i)) && array.valueAt(i)) {
						long itemId = cursor.getLong(cursor
								.getColumnIndex(Contacts._ID));
						Uri uri = Uri.withAppendedPath(TContacts.CONTENT_URI,
								String.valueOf(itemId));
						if (uri != null) {
							ContentValues values = new ContentValues();
							values.put("sync_state",
									SyncState.SYNC_STATE_RECOVER);
							mQueryHandler.startUpdate(TOKEN_FRESH_CONTACTS,
									null, uri, values, null, null);
						}
					}
				}
			}
			cursor.close();
			finish();
			break;
		}
		case MENU_CLEAR: {
			SparseBooleanArray array = listView.getCheckedItemPositions();
			Cursor cursor = this.mAdapter.getCursor();
			if (cursor != null || array.size() > 0) {
				for (int i = 0; i < array.size(); i++) {
					if (cursor.moveToPosition(array.keyAt(i)) && array.valueAt(i)) {
						long itemId = cursor.getLong(cursor
								.getColumnIndex(Contacts._ID));
						Uri uri = Uri.withAppendedPath(TContacts.CONTENT_URI,
								String.valueOf(itemId));
						if (uri != null) {
							ContentValues values = new ContentValues();
							values.put("sync_state",
									SyncState.SYNC_STATE_REMOVE);
							mQueryHandler.startUpdate(TOKEN_FRESH_CONTACTS,
									null, uri, values, null, null);
						}
					}
				}
			}
			cursor.close();
			break;
		}
		case MENU_CANCEL:
			finish();
			break;
		}
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();
	    //todo:不必每次进来都要请求服务器数据。
		//startRetriveContacts();
		startAsyncQuery();
	}

	private Uri recoverOneContact(ContactStruct vcard, final String action,
			boolean recover) {
		Log.d("MyContact", "recover guid " + vcard.guid);
		final Uri resultRawUri = vcard
				.pushIntoContentResolver(getContentResolver());
		final Uri contactLookupUri = TRawContacts.getContactLookupUri(
				getContentResolver(), resultRawUri);
		// FIXME: should auto do this thing
		ContactsUtility.setGuid(this, contactLookupUri, vcard.guid);

		if (!recover && contactLookupUri != null) {
			ContactsUtility.markContact(RecoverActivity.this,
					contactLookupUri, SyncState.SYNC_STATE_DELETE);
		}
		return contactLookupUri;
	}
	
	
	@Override
	protected void onStop() {
		super.onStop();
		//todo：没必要退出时删除本地数据。
		
//		getContentResolver().delete(
//				TRawContacts.CONTENT_URI, "sync_state='"+SyncState.SYNC_STATE_DELETE+"'", null);
	}

	private void startAsyncQuery() {
		try {
			setProgressBarIndeterminateVisibility(true);
			mQueryHandler.startQuery(TOKEN_DELETED_CONTACTS, null,
					TContacts.CONTENT_URI, PROJECTION, "sync_state = '"
							+ SyncState.SYNC_STATE_DELETED
							+ "' OR sync_state = '"
							+ SyncState.SYNC_STATE_DELETE + "'", null, null);
		} catch (SQLiteException e) {

		}
	}

	private final class DeleteContactsQueryHandler extends AsyncQueryHandler {

		public DeleteContactsQueryHandler(ContentResolver contentResolver) {
			super(contentResolver);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			switch (token) {
			case TOKEN_DELETED_CONTACTS:
				mAdapter.changeCursor(cursor);
				setProgressBarIndeterminateVisibility(false);
				break;
			default:
				break;
			}
		}
	}

	//added by zhangbing@inspurworld.com
	@Override
	public void onClick(View paramView) {
		//final ListView listView = getListView();
		//恢复按钮 
		if (paramView==recoverBtn){
			SparseBooleanArray array = listView.getCheckedItemPositions();
			if(array.size()==0)	return;
			
			Cursor cursor = this.mAdapter.getCursor();
			if (cursor != null || array.size() > 0) {
				for (int i = 0; i < array.size(); i++) {
					if (cursor.moveToPosition(array.keyAt(i)) && array.valueAt(i)) {
						long itemId = cursor.getLong(cursor
								.getColumnIndex(Contacts._ID));
						Uri uri = Uri.withAppendedPath(TContacts.CONTENT_URI,
								String.valueOf(itemId));
						if (uri != null) {
							ContentValues values = new ContentValues();
							values.put("sync_state",
									SyncState.SYNC_STATE_RECOVER);
							mQueryHandler.startUpdate(TOKEN_FRESH_CONTACTS,
									null, uri, values, null, null);
						}
					}
				}
			}
			cursor.close();
			finish();
		}
		//彻底删除按钮
		if (paramView==removeBtn){
			//todo: show confirm dialog
			SparseBooleanArray array = listView.getCheckedItemPositions();
			if(array.size()!=0)	showDialog(0);
			
		}
		
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		
		switch (id) {
		case 0:
			// TODO: create show dialog
			return new AlertDialog.Builder(this)
			.setTitle(R.string.menu_remove)
			.setCancelable(false)
			.setMessage(R.string.remove_msg)
			.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int which) {
							SparseBooleanArray array = listView.getCheckedItemPositions();
							Cursor cursor = RecoverActivity.this.mAdapter.getCursor();
							if (cursor != null || array.size() > 0) {
								for (int i = 0; i < array.size(); i++) {
									if (cursor.moveToPosition(array.keyAt(i)) && array.valueAt(i)) {
										long itemId = cursor.getLong(cursor
												.getColumnIndex(Contacts._ID));
										Uri uri = Uri.withAppendedPath(TContacts.CONTENT_URI,
												String.valueOf(itemId));
										if (uri != null) {
											ContentValues values = new ContentValues();
											values.put("sync_state",
													SyncState.SYNC_STATE_REMOVE);
											mQueryHandler.startUpdate(TOKEN_FRESH_CONTACTS,
													null, uri, values, null, null);
										}
									}
								}
							}
							cursor.close();
						}
					})
			.setNegativeButton(android.R.string.cancel,null)
			.create();
			

		default:
			break;
		}
		
		return super.onCreateDialog(id);
	}
}
