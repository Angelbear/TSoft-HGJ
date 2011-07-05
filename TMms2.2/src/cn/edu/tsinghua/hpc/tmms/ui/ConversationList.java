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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.bouncycastle.util.encoders.Base64;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import cn.edu.tsinghua.hpc.google.tmms.pdu.PduHeaders;
import cn.edu.tsinghua.hpc.syncbroker.ElementNotFound;
import cn.edu.tsinghua.hpc.syncbroker.HttpCommunication;
import cn.edu.tsinghua.hpc.syncbroker.SMSRecord;
import cn.edu.tsinghua.hpc.syncbroker.SyncRecord;
import cn.edu.tsinghua.hpc.tmms.LogTag;
import cn.edu.tsinghua.hpc.tmms.MmsConfig;
import cn.edu.tsinghua.hpc.tmms.R;
import cn.edu.tsinghua.hpc.tmms.data.Contact;
import cn.edu.tsinghua.hpc.tmms.data.ContactList;
import cn.edu.tsinghua.hpc.tmms.data.Conversation;
import cn.edu.tsinghua.hpc.tmms.provider.MmsSmsDatabaseHelper;
import cn.edu.tsinghua.hpc.tmms.syncaction.Const;
import cn.edu.tsinghua.hpc.tmms.syncaction.MmsUtils;
import cn.edu.tsinghua.hpc.tmms.syncaction.SyncAction;
import cn.edu.tsinghua.hpc.tmms.syncaction.SyncState;
import cn.edu.tsinghua.hpc.tmms.transaction.MessagingNotification;
import cn.edu.tsinghua.hpc.tmms.transaction.SmsRejectedReceiver;
import cn.edu.tsinghua.hpc.tmms.util.DraftCache;
import cn.edu.tsinghua.hpc.tmms.util.Recycler;
import cn.edu.tsinghua.hpc.tmms.util.TIntent;
import cn.edu.tsinghua.hpc.tmms.util.TMmsSms2;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMmsSms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TSms;

import com.ccit.phone.CCITSC;
import com.ccit.phone.LoginView;

/**
 * This activity provides a list view of existing conversations.
 */
public class ConversationList extends ListActivity implements
		DraftCache.OnDraftChangedListener {
	private static final String TAG = "ConversationList";
	private static final boolean DEBUG = false;
	private static final boolean LOCAL_LOGV = DEBUG;

	private static final int THREAD_LIST_QUERY_TOKEN = 1701;
	public static final int DELETE_CONVERSATION_TOKEN = 1801;
	public static final int HAVE_LOCKED_MESSAGES_TOKEN = 1802;
	private static final int DELETE_OBSOLETE_THREADS_TOKEN = 1803;

	/**
	 * Add by Yangyang Zhao: Token for final delete all
	 */
	public static final int HAVE_LOCKED_MESSAGES_TOKEN_2 = 1804;

	// IDs of the main menu items.
	public static final int MENU_COMPOSE_NEW = 0;
	public static final int MENU_SEARCH = 1;
	public static final int MENU_DELETE_ALL = 3;
	public static final int MENU_PREFERENCES = 4;
	public static final int MENU_HELP = 5;
	public static final int MENU_ABOUT = 6;
	public static final int MENU_CONTACT = 7;
	// public static final int MENU_FINAL_DELETE_ALL = 5;
	public static final int MENU_RECYCLE_BIN = 8;
	public static final int MENU_MORE_THREAD = 9;
	public static final int MENU_RESYNC = 10;
	public static final int MENU_WIPEDATA = 11;

	// IDs of the context menu items for the list of conversations.
	public static final int MENU_DELETE = 0;
	public static final int MENU_VIEW = 1;
	public static final int MENU_VIEW_CONTACT = 2;
	public static final int MENU_ADD_TO_CONTACTS = 3;
	// private static final int MENU_FINAL_DELETE = 5;
	// private static final int MENU_MARK_PRIVATE = 6;
	public static final int MENU_MORE_MESSAGES = 7;

	private ThreadListQueryHandler mQueryHandler;
	private ConversationListAdapter mListAdapter;
	private CharSequence mTitle;
	private SharedPreferences mPrefs;
	private Handler mHandler;
	private boolean mNeedToMarkAsSeen;
	private Context mContext;
	private String mMessage;
	private int mTotal;
	static private final String CHECKED_MESSAGE_LIMITS = "checked_message_limits";

	public static final int DIALOG_LOGINING_ID = 0;
	public static final int DIALOG_LOGIN_FAILED_ID = 1;
	public static final int DIALOG_FIRST_SYNCHRONIZATION_ID = 2;
	public static final int DIALOG_FIRST_SYNCHRONIZING_ID = 3;
	public static final int DIALOG_FIRST_SYNCHONIZATION_SUCCEED_ID = 4;
	public static final int DIALOG_FIRST_SYNCHONIZATION_FAILED_ID = 5;
	private final Handler mHandler2 = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			if (b == null || b.isEmpty()) {
				ConversationList.this
						.dismissDialog(DIALOG_FIRST_SYNCHRONIZING_ID);
				switch (msg.what) {
				case 0: {
					showDialog(DIALOG_FIRST_SYNCHONIZATION_SUCCEED_ID);
					break;
				}
				case 1: {
					ConversationList.this.mMessage = getString(R.string.retry_and_continue_sychonizing_button);
					showDialog(DIALOG_FIRST_SYNCHONIZATION_FAILED_ID);
					break;
				}
				case 2: {
					ConversationList.this.mMessage = getString(R.string.recover_failed_message);
					showDialog(DIALOG_FIRST_SYNCHONIZATION_FAILED_ID);
					break;
				}
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		TIntent.checkTContacts(mContext);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.conversation_list_screen);

		mQueryHandler = new ThreadListQueryHandler(getContentResolver());

		ListView listView = getListView();
		LayoutInflater inflater = LayoutInflater.from(this);
		ConversationListItem headerView = (ConversationListItem) inflater
				.inflate(R.layout.conversation_list_item, listView, false);
		headerView.bind(getString(R.string.new_message),
				getString(R.string.create_new_message));
		listView.addHeaderView(headerView, null, true);

		listView.setOnCreateContextMenuListener(mConvListOnCreateContextMenuListener);
		listView.setOnKeyListener(mThreadListKeyListener);

		initListAdapter();

		mTitle = getString(R.string.app_label);

		mHandler = new Handler();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean checkedMessageLimits = mPrefs.getBoolean(
				CHECKED_MESSAGE_LIMITS, false);
		if (DEBUG)
			Log.v(TAG, "checkedMessageLimits: " + checkedMessageLimits);
		if (!checkedMessageLimits || DEBUG) {
			runOneTimeStorageLimitCheckForLegacyMessages();
		}
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			MmsConfig.setTSyncEnabled(true);
			if (MmsConfig.isFirstLaunch() || MmsConfig.getSessionID() == null) {
				new LoginThread(TMmsSms2.LOGIN_TYPE_FIRST).start();

			} else {
				// new LoginThread(TMmsSms2.LOGIN_TYPE_NORMAL).start();
				HttpCommunication.initHttpParameter("Sessionid",
						MmsConfig.getSessionID());// 12.14
			}
		} else {
			MmsConfig.setTSyncEnabled(false);
		}

		setTheme(android.R.style.Theme_Black);
	}

	private class LoginThread extends Thread {
		int loginType;

		public LoginThread(int loginType) {
			this.loginType = loginType;
			// showDialog(DIALOG_LOGINING_ID);
			getWaitingDialog("正在登陆请稍后......").show();
		}

		@Override
		public void run() {
			// Looper.prepare();
			try {
				Log.d(TAG, "--------LoginThread----run-----------");
				CCITSC mCCIT = new CCITSC(ConversationList.this, Const.CAIP,
						Const.CAPORT);
				mCCIT.loginInit(false);
				LoginView lv = mCCIT.requestLogin(false);

				Message msg = dialogShowHandler.obtainMessage();
				if (null != lv) {
					msg.obj = lv;
					msg.what = TMmsSms2.LOGIN_SUCCESS;
					Bundle b = new Bundle();
					if (this.loginType == TMmsSms2.LOGIN_TYPE_FIRST) {
						b.putInt(TMmsSms2.KEY_SYNC_TYPE,
								TMmsSms2.SYNC_TYPE_FIRST);
					} else if (this.loginType == TMmsSms2.LOGIN_TYPE_RELOGIN) {

						b.putInt(TMmsSms2.KEY_SYNC_TYPE,
								TMmsSms2.SYNC_TYPE_RESYNC);
					} else if (this.loginType == TMmsSms2.LOGIN_TYPE_NORMAL) {

						b.putInt(TMmsSms2.KEY_SYNC_TYPE,
								TMmsSms2.SYNC_TYPE_NOSYNC);

					}
					msg.setData(b);
					dialogShowHandler.sendMessage(msg);

				} else {
					Bundle b = new Bundle();
					b.putInt(TMmsSms2.KEY_ERROR_CODE,
							TMmsSms2.ERROR_CODE_LOGIN_FAILURE);
					b.putString(TMmsSms2.KEY_ERROR_MSG, "CA验证失败");
					msg.what = R.id.dialog_sync_failed;
					msg.setData(b);
					dialogShowHandler.sendMessage(msg);
				}

			} catch (Exception e) {
				e.printStackTrace();
				Message msg = dialogShowHandler.obtainMessage();
				String errorMsg = e.getMessage();
				Bundle b = new Bundle();
				b.putInt(TMmsSms2.KEY_ERROR_CODE,
						TMmsSms2.ERROR_CODE_LOGIN_FAILURE);
				b.putString(TMmsSms2.KEY_ERROR_MSG, errorMsg);
				// b.putString(TMmsSms2.KEY_ERROR_MSG, "CA验证失败");
				msg.what = R.id.dialog_sync_failed;
				msg.setData(b);
				dialogShowHandler.sendMessage(msg);
			}

		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder builder = null;
		switch (id) {
		// user logining progress dialog
		case DIALOG_LOGINING_ID: {

			waitingDialog = new ProgressDialog(this);
			waitingDialog.setMessage("正在登陆请稍后......");
			waitingDialog.setIndeterminate(true);
			waitingDialog.setCancelable(true);
			return waitingDialog;
		}
			//
		case DIALOG_FIRST_SYNCHRONIZATION_ID: {
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.first_synchonization_title);
			builder.setMessage(R.string.first_synchronization_message);
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.yes_and_continue_button,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();

							// runFirstSync();
						}
					});
			builder.setNegativeButton(R.string.no_and_cance_button,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							MmsConfig.setTSyncEnabled(false);
						}
					});
			dialog = builder.create();
			break;
		}

		case DIALOG_FIRST_SYNCHONIZATION_SUCCEED_ID: {
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.first_synchonization_title);
			builder.setMessage(R.string.first_synchronization_succeed_message);
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.complete_button,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			dialog = builder.create();
			break;
		}

		case DIALOG_FIRST_SYNCHONIZATION_FAILED_ID: {
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.first_synchonization_title);
			builder.setMessage(mMessage);
			builder.setCancelable(false);
			builder.setPositiveButton(
					R.string.retry_and_continue_sychonizing_button,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							// runFirstSync();
						}
					});
			builder.setNegativeButton(
					R.string.abandon_and_enter_main_screen_button,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							MmsConfig.setTSyncEnabled(false);
						}
					});
			dialog = builder.create();
			break;
		}

		default: {
		}

		}

		return dialog;
	}

	private final ConversationListAdapter.OnContentChangedListener mContentChangedListener = new ConversationListAdapter.OnContentChangedListener() {
		public void onContentChanged(ConversationListAdapter adapter) {
			startAsyncQuery();
		}
	};

	private void initListAdapter() {
		mListAdapter = new ConversationListAdapter(this, null);
		mListAdapter.setOnContentChangedListener(mContentChangedListener);
		setListAdapter(mListAdapter);
		getListView().setRecyclerListener(mListAdapter);
	}

	/**
	 * Checks to see if the number of MMS and SMS messages are under the limits
	 * for the recycler. If so, it will automatically turn on the recycler
	 * setting. If not, it will prompt the user with a message and point them to
	 * the setting to manually turn on the recycler.
	 */
	public synchronized void runOneTimeStorageLimitCheckForLegacyMessages() {
		if (Recycler.isAutoDeleteEnabled(this)) {
			if (DEBUG)
				Log.v(TAG, "recycler is already turned on");
			// The recycler is already turned on. We don't need to check
			// anything or warn
			// the user, just remember that we've made the check.
			markCheckedMessageLimit();
			return;
		}
		new Thread(new Runnable() {
			public void run() {
				if (Recycler.checkForThreadsOverLimit(ConversationList.this)) {
					if (DEBUG)
						Log.v(TAG, "checkForThreadsOverLimit TRUE");
					// Dang, one or more of the threads are over the limit. Show
					// an activity
					// that'll encourage the user to manually turn on the
					// setting. Delay showing
					// this activity until a couple of seconds after the
					// conversation list appears.
					mHandler.postDelayed(new Runnable() {
						public void run() {
							Intent intent = new Intent(ConversationList.this,
									WarnOfStorageLimitsActivity.class);
							startActivity(intent);
						}
					}, 2000);
				} else {
					if (DEBUG)
						Log.v(TAG,
								"checkForThreadsOverLimit silently turning on recycler");
					// No threads were over the limit. Turn on the recycler by
					// default.
					runOnUiThread(new Runnable() {
						public void run() {
							SharedPreferences.Editor editor = mPrefs.edit();
							editor.putBoolean(
									MessagingPreferenceActivity.AUTO_DELETE,
									true);
							editor.commit();
						}
					});
				}
				// Remember that we don't have to do the check anymore when
				// starting MMS.
				runOnUiThread(new Runnable() {
					public void run() {
						markCheckedMessageLimit();
					}
				});
			}
		}).start();
	}

	/**
	 * Mark in preferences that we've checked the user's message limits. Once
	 * checked, we'll never check them again, unless the user wipe-data or
	 * resets the device.
	 */
	private void markCheckedMessageLimit() {
		if (DEBUG)
			Log.v(TAG, "markCheckedMessageLimit");
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putBoolean(CHECKED_MESSAGE_LIMITS, true);
		editor.commit();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// Handle intents that occur after the activity has already been
		// created.
		startAsyncQuery();
	}

	@Override
	protected void onStart() {
		super.onStart();

		MessagingNotification.cancelNotification(getApplicationContext(),
				SmsRejectedReceiver.SMS_REJECTED_NOTIFICATION_ID);

		DraftCache.getInstance().addOnDraftChangedListener(this);

		mNeedToMarkAsSeen = true;

		startAsyncQuery();

		// We used to refresh the DraftCache here, but
		// refreshing the DraftCache each time we go to the ConversationList
		// seems overly
		// aggressive. We already update the DraftCache when leaving CMA in
		// onStop() and
		// onNewIntent(), and when we delete threads or delete all in CMA or
		// this activity.
		// I hope we don't have to do such a heavy operation each time we enter
		// here.

		// we invalidate the contact cache here because we want to get updated
		// presence
		// and any contact changes. We don't invalidate the cache by observing
		// presence and contact
		// changes (since that's too untargeted), so as a tradeoff we do it
		// here.
		// If we're in the middle of the app initialization where we're loading
		// the conversation
		// threads, don't invalidate the cache because we're in the process of
		// building it.
		// TODO: think of a better way to invalidate cache more surgically or
		// based on actual
		// TODO: changes we care about
		if (!Conversation.loadingThreads()) {
			Contact.invalidateCache();
		}
		// //add by chenqiang
		// dismissDialog(0);
	}

	@Override
	protected void onRestart() {
		Log.i(TAG, "-------onRestart--------");
		super.onRestart();
		// add by chenqiang:设置中打开同步后返回主界面时执行登录
		if (MmsConfig.isFirstLaunch()) {
			MmsConfig.setFirstLaunch(false);
			MmsConfig.setTSyncEnabled(true);
		}

		SharedPreferences sharedata = getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms_preferences", 0);
		boolean sync_state = sharedata.getBoolean(
				"pref_key_mms_transparent_sync", true);

		if (MmsConfig.isTSyncEnabled() && sync_state
				&& !MessagingPreferenceActivity.oldsync_state) {
			new LoginThread(TMmsSms2.LOGIN_TYPE_FIRST).start();
			MessagingPreferenceActivity.oldsync_state = !MessagingPreferenceActivity.oldsync_state;
		}
		privateOnStart();
	}

	protected void privateOnStart() {
		startAsyncQuery();
	}

	@Override
	protected void onResume() {
		super.onResume();
		privateOnStart();
	}

	@Override
	protected void onStop() {
		super.onStop();

		DraftCache.getInstance().removeOnDraftChangedListener(this);
		mListAdapter.changeCursor(null);
	}

	public void onDraftChanged(final long threadId, final boolean hasDraft) {
		// Run notifyDataSetChanged() on the main thread.
		mQueryHandler.post(new Runnable() {
			public void run() {
				if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
					log("onDraftChanged: threadId=" + threadId + ", hasDraft="
							+ hasDraft);
				}
				mListAdapter.notifyDataSetChanged();
			}
		});
	}

	private void startAsyncQuery() {
		try {
			setTitle(getString(R.string.refreshing));
			setProgressBarIndeterminateVisibility(true);

			Conversation.startQueryForAll(mQueryHandler,
					THREAD_LIST_QUERY_TOKEN);
		} catch (SQLiteException e) {
			SqliteWrapper.checkSQLiteException(this, e);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();

		menu.add(0, MENU_COMPOSE_NEW, 0, R.string.menu_compose_new).setIcon(
				com.android.internal.R.drawable.ic_menu_compose);

		if (mListAdapter.getCount() > 0) {
			menu.add(0, MENU_DELETE_ALL, 0, R.string.menu_delete_all).setIcon(
					android.R.drawable.ic_menu_delete);
		}
		menu.add(0, MENU_RECYCLE_BIN, 0, R.string.menu_recycle_bin).setIcon(
				R.drawable.ic_menu_recovery_message);
		menu.add(0, MENU_SEARCH, 0, android.R.string.search_go)
				.setIcon(android.R.drawable.ic_menu_search)
				.setAlphabeticShortcut(android.app.SearchManager.MENU_KEY);
		menu.add(0, MENU_MORE_THREAD, 0, R.string.menu_synchonize_more_thread)
				.setIcon(android.R.drawable.ic_menu_more);

		menu.add(0, MENU_PREFERENCES, 0, R.string.menu_preferences).setIcon(
				android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_HELP, 0, R.string.menu_help).setIcon(
				android.R.drawable.ic_menu_agenda);
		menu.add(0, MENU_ABOUT, 0, R.string.menu_about).setIcon(
				android.R.drawable.ic_menu_agenda);
		menu.add(0, MENU_CONTACT, 0, R.string.menu_contact).setIcon(
				android.R.drawable.ic_menu_agenda);

		return true;
	}

	@Override
	public boolean onSearchRequested() {
		startSearch(null, false, null /* appData */, false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_COMPOSE_NEW:
			createNewMessage();
			break;
		case MENU_SEARCH:
			onSearchRequested();
			break;
		case MENU_DELETE_ALL:
			// The invalid threadId of -1 means all threads here.
			confirmDeleteThread(-1L, mQueryHandler);
			break;
		case MENU_PREFERENCES: {
			Intent intent = new Intent(this, MessagingPreferenceActivity.class);
			startActivityIfNeeded(intent, -1);
			break;
		}
		case MENU_RECYCLE_BIN:
			Intent newIntent = new Intent(this, RecoverThreadActivity.class);
			startActivityIfNeeded(newIntent, -1);
			break;

		case MENU_MORE_THREAD:

			resync();
			break;
		case MENU_WIPEDATA:
			MmsSmsDatabaseHelper.getInstance(this).wipeData();
			MmsConfig.wipedata(this);
			this.finish();
			break;
		case MENU_HELP:{
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
					.setTitle(getString(R.string.menu_help))
					.setPositiveButton(getString(android.R.string.ok), null)
					.setMessage(getString(R.string.help_text));
			builder.create().show();
			break;
		}

		case MENU_ABOUT:{
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
					.setTitle(getString(R.string.menu_about))
					.setPositiveButton(getString(android.R.string.ok), null)
					.setMessage(getString(R.string.about_text));
			builder.create().show();
		}
			break;
		case MENU_CONTACT:{
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
					.setTitle(getString(R.string.menu_contact))
					.setPositiveButton(getString(android.R.string.ok), null)
					.setMessage(getString(R.string.contact_us));
			builder.create().show();
		}
			break;
		default:
			return true;
		}
		return false;
	}

	/**
	 * 重新同步
	 */
	private void resync() {
		// TODO Auto-generated method stub
		if (MmsConfig.isTSyncEnabled() == false
				|| MmsConfig.getSessionID() == null) {
			new LoginThread(TMmsSms2.LOGIN_TYPE_RELOGIN).start();
		} else {
			sync(TMmsSms2.SYNC_TYPE_RESYNC);// 12.14
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (LOCAL_LOGV) {
			Log.v(TAG, "onListItemClick: position=" + position + ", id=" + id);
		}

		if (position == 0) {
			createNewMessage();
		} else if (v instanceof ConversationListItem) {
			ConversationListItem headerView = (ConversationListItem) v;
			ConversationListItemData ch = headerView.getConversationHeader();

			if (ch.getThreadId() != -2) {
				Log.d("Mms", "open localThread");
				openThread(ch.getThreadId());
			} else {
				Log.d("Mms", "open remoteThread");
				if (l.getAdapter().getItem(position) instanceof SMSRecord) {
					SMSRecord s = (SMSRecord) l.getAdapter().getItem(position);
					Uri uri = MmsUtils.tempInsertOneThreadInto(
							ConversationList.this, s);
					Log.d("Mms", "open thread is " + uri.toString());
					long threadId = ContentUris.parseId(uri);
					Log.d("Mms", "open thread is " + threadId);
					openRemoteThread(threadId);
				}
				// openRemoteThread(ch.getThreadId(), ch.getRecipentNumber());
			}
		}
	}

	private void createNewMessage() {
		startActivity(ComposeMessageActivity.createIntent(this, 0));
	}

	private void openThread(long threadId) {
		startActivity(ComposeMessageActivity.createIntent(this, threadId));
	}

	private void openRemoteThread(long threadId) {
		startActivity(ComposeMessageActivity.createIntent(this, threadId));
	}

	public static Intent createAddContactIntent(String address) {
		// address must be a single recipient
		Intent intent = new Intent(TIntent.ACTION_INSERT_OR_EDIT);
		intent.setType(Contacts.CONTENT_ITEM_TYPE);
		if (Mms.isEmailAddress(address)) {
			intent.putExtra(ContactsContract.Intents.Insert.EMAIL, address);
		} else {
			intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
			intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
					ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
		}
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

		return intent;
	}

	private final OnCreateContextMenuListener mConvListOnCreateContextMenuListener = new OnCreateContextMenuListener() {
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {
			Cursor cursor = mListAdapter.getCursor();
			if (cursor == null || cursor.getPosition() < 0) {
				return;
			}
			Conversation conv = Conversation
					.from(ConversationList.this, cursor);
			ContactList recipients = conv.getRecipients();
			menu.setHeaderTitle(recipients.formatNames(","));

			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			if (info.position > 0) {
				menu.add(0, MENU_VIEW, 0, R.string.menu_view);

				// Only show if there's a single recipient
				if (recipients.size() == 1) {
					// do we have this recipient in contacts?
					if (recipients.get(0).existsInDatabase()) {
						menu.add(0, MENU_VIEW_CONTACT, 0,
								R.string.menu_view_contact);
					} else {
						menu.add(0, MENU_ADD_TO_CONTACTS, 0,
								R.string.menu_add_to_contacts);
					}
				}
				menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
				menu.add(0, MENU_MORE_MESSAGES, 0,
						R.string.menu_synchonize_more_message);
			}
		}
	};

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Cursor cursor = mListAdapter.getCursor();
		if (cursor != null && cursor.getPosition() >= 0) {
			Conversation conv = Conversation
					.from(ConversationList.this, cursor);
			long threadId = conv.getThreadId();
			switch (item.getItemId()) {
			case MENU_DELETE: {
				confirmDeleteThread(threadId, mQueryHandler);
				break;
			}
			case MENU_VIEW: {
				openThread(threadId);
				break;
			}
			case MENU_VIEW_CONTACT: {
				Contact contact = conv.getRecipients().get(0);
				Intent intent = new Intent(TIntent.ACTION_VIEW,
						contact.getUri());
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(intent);
				break;
			}
			case MENU_ADD_TO_CONTACTS: {
				String address = conv.getRecipients().get(0).getNumber();
				startActivity(createAddContactIntent(address));
				break;
			}
			case MENU_MORE_MESSAGES: {
				// / TODO
				break;
			}
			default:
				break;
			}
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// We override this method to avoid restarting the entire
		// activity when the keyboard is opened (declared in
		// AndroidManifest.xml). Because the only translatable text
		// in this activity is "New Message", which has the full width
		// of phone to work with, localization shouldn't be a problem:
		// no abbreviated alternate words should be needed even in
		// 'wide' languages like German or Russian.

		super.onConfigurationChanged(newConfig);
		if (DEBUG)
			Log.v(TAG, "onConfigurationChanged: " + newConfig);
	}

	/**
	 * Start the process of putting up a dialog to confirm deleting a thread,
	 * but first start a background query to see if any of the threads or thread
	 * contain locked messages so we'll know how detailed of a UI to display.
	 * 
	 * @param threadId
	 *            id of the thread to delete or -1 for all threads
	 * @param handler
	 *            query handler to do the background locked query
	 */
	public static void confirmDeleteThread(long threadId,
			AsyncQueryHandler handler) {
		Conversation.startQueryHaveLockedMessages(handler, threadId,
				HAVE_LOCKED_MESSAGES_TOKEN);
	}

	public static void confirmFinalDeleteThread(long threadId,
			AsyncQueryHandler handler) {
		Conversation.startQueryHaveLockedMessages(handler, threadId,
				HAVE_LOCKED_MESSAGES_TOKEN_2);
	}

	/**
	 * Build and show the proper delete thread dialog. The UI is slightly
	 * different depending on whether there are locked messages in the thread(s)
	 * and whether we're deleting a single thread or all threads.
	 * 
	 * @param listener
	 *            gets called when the delete button is pressed
	 * @param deleteAll
	 *            whether to show a single thread or all threads UI
	 * @param hasLockedMessages
	 *            whether the thread(s) contain locked messages
	 * @param context
	 *            used to load the various UI elements
	 */
	public static void confirmDeleteThreadDialog(
			final DeleteThreadListener listener, boolean deleteAll,
			boolean hasLockedMessages, Context context) {
		View contents = View.inflate(context,
				R.layout.delete_thread_dialog_view, null);
		TextView msg = (TextView) contents.findViewById(R.id.message);
		msg.setText(deleteAll ? R.string.confirm_delete_all_conversations
				: R.string.confirm_delete_conversation);
		final CheckBox checkbox = (CheckBox) contents
				.findViewById(R.id.delete_locked);
		if (!hasLockedMessages) {
			checkbox.setVisibility(View.GONE);
		} else {
			listener.setDeleteLockedMessage(checkbox.isChecked());
			checkbox.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					listener.setDeleteLockedMessage(checkbox.isChecked());
				}
			});
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.confirm_dialog_title)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(true)
				.setPositiveButton(R.string.delete, listener)
				.setNegativeButton(R.string.no, null).setView(contents).show();
	}

	public static void confirmFinalDeleteThreadDialog(
			final FinalDeleteThreadListener listener, boolean deleteAll,
			boolean hasLockedMessages, Context context) {
		View contents = View.inflate(context,
				R.layout.delete_thread_dialog_view, null);
		TextView msg = (TextView) contents.findViewById(R.id.message);
		msg.setText(deleteAll ? R.string.confirm_delete_all_conversations
				: R.string.confirm_delete_conversation);
		final CheckBox checkbox = (CheckBox) contents
				.findViewById(R.id.delete_locked);
		if (!hasLockedMessages) {
			checkbox.setVisibility(View.GONE);
		} else {
			listener.setDeleteLockedMessage(checkbox.isChecked());
			checkbox.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					listener.setDeleteLockedMessage(checkbox.isChecked());
				}
			});
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.confirm_dialog_title)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(true)
				.setPositiveButton(R.string.delete, listener)
				.setNegativeButton(R.string.no, null).setView(contents).show();
	}

	private final OnKeyListener mThreadListKeyListener = new OnKeyListener() {
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				switch (keyCode) {
				case KeyEvent.KEYCODE_DEL: {
					long id = getListView().getSelectedItemId();
					if (id > 0) {
						confirmDeleteThread(id, mQueryHandler);
					}
					return true;
				}
				}
			}
			return false;
		}
	};

	public static class DeleteThreadListener implements OnClickListener {
		private final long mThreadId;
		private final AsyncQueryHandler mHandler;
		private final Context mContext;
		private boolean mDeleteLockedMessages;

		public DeleteThreadListener(long threadId, AsyncQueryHandler handler,
				Context context) {
			mThreadId = threadId;
			mHandler = handler;
			mContext = context;
		}

		public void setDeleteLockedMessage(boolean deleteLockedMessages) {
			mDeleteLockedMessages = deleteLockedMessages;
		}

		public void onClick(DialogInterface dialog, final int whichButton) {
			MessageUtils.handleReadReport(mContext, mThreadId,
					PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ,
					new Runnable() {
						public void run() {
							int token = DELETE_CONVERSATION_TOKEN;
							if (mThreadId == -1) {
								Conversation.startDeleteAll(mHandler, token,
										mDeleteLockedMessages, false);
								// if
								// (!MmsUtils.hasThreadLockedMessages(mContext,
								// mThreadId)
								// || mDeleteLockedMessages) {
								// Uri mDeleteUri = Uri.withAppendedPath(
								// TMmsSms.AUTHORITY_URI,
								// "all-threads");
								// //
								// Uri.parse("content://mms-sms/all-threads/");
								// MmsUtils.markMessageOrThread(mContext,
								// mDeleteUri,
								// SyncState.SYNC_STATE_DELETED);
								// //add by chenqiang
								SQLiteOpenHelper mOpenHelper = MmsSmsDatabaseHelper
										.getInstance(mContext);
								SQLiteDatabase db = mOpenHelper
										.getWritableDatabase();
								ContentValues values = new ContentValues();
								values.put("sync_state",
										SyncState.SYNC_STATE_DELETED);
								db.update("sms", values, "sync_state!='"
										+ SyncState.SYNC_STATE_REMOVED + "'",
										null);
								db.update("pdu", values, "sync_state!='"
										+ SyncState.SYNC_STATE_REMOVED + "'",
										null);
								// }

								DraftCache.getInstance().refresh();
							} else {
								Conversation
										.startDelete(mHandler, token,
												mDeleteLockedMessages,
												mThreadId, false);
								// if
								// (!MmsUtils.hasThreadLockedMessages(mContext,
								// mThreadId)
								// || mDeleteLockedMessages) {
								// Uri mDeleteUri = ContentUris
								// .withAppendedId(Uri.withAppendedPath(
								// TMmsSms.AUTHORITY_URI,
								// "threads"),
								// // Uri.parse("content://mms-sms/threads/"),
								// mThreadId);
								// MmsUtils.markMessageOrThread(mContext,
								// mDeleteUri,
								// SyncState.SYNC_STATE_DELETED);
								//
								// }
								DraftCache.getInstance().setDraftState(
										mThreadId, false);
							}
						}
					});
		}
	}

	public static class FinalDeleteThreadListener implements OnClickListener {
		private final long mThreadId;
		private final AsyncQueryHandler mHandler;
		private final Context mContext;
		private boolean mDeleteLockedMessages;

		public FinalDeleteThreadListener(long threadId,
				AsyncQueryHandler handler, Context context) {
			mThreadId = threadId;
			mHandler = handler;
			mContext = context;
		}

		public void setDeleteLockedMessage(boolean deleteLockedMessages) {
			mDeleteLockedMessages = deleteLockedMessages;
		}

		public void onClick(DialogInterface dialog, final int whichButton) {
			MessageUtils.handleReadReport(mContext, mThreadId,
					PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ,
					new Runnable() {
						public void run() {
							int token = DELETE_CONVERSATION_TOKEN;
							if (mThreadId == -1) {
								Conversation.startDeleteAll(mHandler, token,
										mDeleteLockedMessages, true);
								if (!MmsUtils.hasThreadLockedMessages(mContext,
										mThreadId) || mDeleteLockedMessages) {
									Uri mDeleteUri = Uri.withAppendedPath(
											TMmsSms.AUTHORITY_URI,
											"all-threads");
									// Uri.parse("content://mms-sms/all-threads/");
									MmsUtils.markMessageOrThread(mContext,
											mDeleteUri,
											SyncState.SYNC_STATE_REMOVED);

								}
								DraftCache.getInstance().refresh();
							} else {
								Conversation.startDelete(mHandler, token,
										mDeleteLockedMessages, mThreadId, true);
								if (!MmsUtils.hasThreadLockedMessages(mContext,
										mThreadId) || mDeleteLockedMessages) {
									Uri mDeleteUri = ContentUris.withAppendedId(
											Uri.withAppendedPath(
													TMmsSms.AUTHORITY_URI,
													"threads"),
											// Uri.parse("content://mms-sms/threads/"),
											mThreadId);
									MmsUtils.markMessageOrThread(mContext,
											mDeleteUri,
											SyncState.SYNC_STATE_REMOVED);

								}
								DraftCache.getInstance().setDraftState(
										mThreadId, false);
							}
						}
					});
		}
	}

	private final class ThreadListQueryHandler extends AsyncQueryHandler {
		public ThreadListQueryHandler(ContentResolver contentResolver) {
			super(contentResolver);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			switch (token) {
			case THREAD_LIST_QUERY_TOKEN:
				mListAdapter.changeCursor(cursor);
				setTitle(mTitle);
				setProgressBarIndeterminateVisibility(false);

				if (mNeedToMarkAsSeen) {
					mNeedToMarkAsSeen = false;
					Conversation
							.markAllConversationsAsSeen(getApplicationContext());

					// Delete any obsolete threads. Obsolete threads are threads
					// that aren't
					// referenced by at least one message in the pdu or sms
					// tables.
					Conversation.asyncDeleteObsoleteThreads(mQueryHandler,
							DELETE_OBSOLETE_THREADS_TOKEN);
				}
				break;

			case HAVE_LOCKED_MESSAGES_TOKEN:
				long threadId = (Long) cookie;
				confirmDeleteThreadDialog(new DeleteThreadListener(threadId,
						mQueryHandler, ConversationList.this), threadId == -1,
						cursor != null && cursor.getCount() > 0,
						ConversationList.this);
				break;
			case HAVE_LOCKED_MESSAGES_TOKEN_2: {
				long threadId1 = (Long) cookie;
				confirmFinalDeleteThreadDialog(new FinalDeleteThreadListener(
						threadId1, mQueryHandler, ConversationList.this),
						threadId1 == -1, cursor != null
								&& cursor.getCount() > 0, ConversationList.this);
				break;
			}

			default:
				Log.e(TAG, "onQueryComplete called with unknown token " + token);
			}
		}

		@Override
		protected void onUpdateComplete(int token, Object cookie, int result) {
			switch (token) {
			case DELETE_CONVERSATION_TOKEN:
				// Make sure the conversation cache reflects the threads in the
				// DB.
				Conversation.init(ConversationList.this);

				// Update the notification for new messages since they
				// may be deleted.
				MessagingNotification
						.updateNewMessageIndicator(ConversationList.this);
				// Update the notification for failed messages since they
				// may be deleted.
				MessagingNotification
						.updateSendFailedNotification(ConversationList.this);

				// Make sure the list reflects the delete
				startAsyncQuery();

				onContentChanged();
				// MmsUtils.notifySyncService(ConversationList.this);
				break;
			}
		}

		@Override
		protected void onDeleteComplete(int token, Object cookie, int result) {
			switch (token) {
			case DELETE_CONVERSATION_TOKEN:
				// Make sure the conversation cache reflects the threads in the
				// DB.
				Conversation.init(ConversationList.this);

				// Update the notification for new messages since they
				// may be deleted.
				MessagingNotification.nonBlockingUpdateNewMessageIndicator(
						ConversationList.this, false, false);
				// Update the notification for failed messages since they
				// may be deleted.
				MessagingNotification
						.updateSendFailedNotification(ConversationList.this);

				// Make sure the list reflects the delete
				startAsyncQuery();
				break;

			case DELETE_OBSOLETE_THREADS_TOKEN:
				// Nothing to do here.
				break;
			}
		}
	}

	private void log(String format, Object... args) {
		String s = String.format(format, args);
		Log.d(TAG, "[" + Thread.currentThread().getId() + "] " + s);
	}

	private DialogShowHandler dialogShowHandler = new DialogShowHandler();

	private class DialogShowHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			// ContactsListActivity.this.dismissDialog(msg.what);
			switch (msg.what) {
			case 0:
				waitingDialog.show();
				break;
			case 1:
				Toast.makeText(ConversationList.this,
						"can not recover from server", 1000).show();
			case 2:
				Toast.makeText(ConversationList.this, R.string.sync_failed,
						1000).show();
				break;
			case 3:// 关闭同步对话框
				syncProgressDialog.cancel();
				if (syncThread != null) {
					syncThread.interrupt();
				}
				break;
			case R.id.dialog_sync_progress: {
				// todo: deal with progress dialog
				// added by zhangbing@inspurworld.com
				if (waitingDialog != null && waitingDialog.isShowing()) {
					waitingDialog.dismiss();
				}

				int total = msg.getData().getInt("total");
				int progress = msg.getData().getInt("progress");
				// syncProgressDialog.setMax(total);
				syncProgressDialog.setProgress(progress);
				if (!syncProgressDialog.isShowing())
					syncProgressDialog.show();

				if (progress >= total) {
					// 同步结束

					if (syncProgressDialog != null
							&& syncProgressDialog.isShowing()) {
						syncProgressDialog.hide();
					}
					return;
				}

				break;

			}
			case R.id.dialog_sync_failed: {
				// 处理登陆失败
				MmsConfig.setTSyncEnabled(false);
				Log.d(TAG, "----------dialog_sync_failed------------");

				if (syncProgressDialog != null
						&& syncProgressDialog.isShowing()) {
					syncProgressDialog.dismiss();
				}
				if (waitingDialog != null && waitingDialog.isShowing()) {
					waitingDialog.dismiss();
				}
				// String errorMsg = (String)msg.obj;
				Bundle b = msg.getData();
				String errorMsg = b.getString(TMmsSms2.KEY_ERROR_MSG);

				if (b.getInt(TMmsSms2.KEY_ERROR_CODE) == TMmsSms2.ERROR_CODE_LOGIN_FAILURE) {

					// getFailedDialog("登录失败", errorMsg,
					// TMmsSms2.ERROR_CODE_LOGIN_FAILURE).show();

				} else if (b.getInt(TMmsSms2.KEY_ERROR_CODE) == TMmsSms2.ERROR_CODE_SYNC_FAILURE) {

					getFailedDialog("同步失败", errorMsg,
							TMmsSms2.ERROR_CODE_SYNC_FAILURE).show();

				}

				break;

			}
			case TMmsSms2.LOGIN_SUCCESS: {
				// 处理登录成功！
				MmsConfig.setTSyncEnabled(true);
				Log.d(TAG, "----------LOGIN_SUCCESS------------");
				if (waitingDialog != null && waitingDialog.isShowing()) {
					waitingDialog.dismiss();
				}
				LoginView loginView = (LoginView) msg.obj;
				String uid = loginView.getUID();
				Log.d(TAG, "---uid------" + uid);
				String token = new String(Base64.encode(loginView
						.getSignature()));
				Bundle b = msg.getData();
				int syncType = b.getInt(TMmsSms2.KEY_SYNC_TYPE);

				// 设置http 头部参数，添加uid和token
				HttpCommunication.initHttpParameter("Uid", uid);
				HttpCommunication.initHttpParameter("Token", token);

				Log.d(TAG, "sync type " + syncType);
				// 判断是否是首次同步，如果不是就进行首次同步，否则就不用进行首次同步了
				if (syncType == TMmsSms2.SYNC_TYPE_FIRST/*
														 * MmsConfig.isFirstLaunch
														 * () || (MmsConfig.
														 * isUserNeedFirstSync(
														 * ConversationList
														 * .this, uid) == 1)
														 */) {
					MmsConfig.setIMSI(mContext);
					MmsConfig.setFirstLaunch(false);
					// first sync
					Log.d(TAG, "first sync");
					sync(TMmsSms2.SYNC_TYPE_FIRST);

				} else if (syncType == TMmsSms2.SYNC_TYPE_RESYNC) {
					Log.d(TAG, "re sync");
					sync(TMmsSms2.SYNC_TYPE_RESYNC);

				} else if (syncType == TMmsSms2.SYNC_TYPE_NOSYNC) {
					Log.d(TAG, "no sync");
					sync(TMmsSms2.SYNC_TYPE_NOSYNC);
				}

				MmsConfig.setUserID(uid);
				MmsConfig.setToken(token);
				break;

			}

			default: {

			}

			}
		}

	}

	private ProgressDialog waitingDialog;
	private ProgressDialog syncProgressDialog;
	private AlertDialog.Builder failedDialog;

	private ProgressDialog getWaitingDialog(String msg) {
		waitingDialog = new ProgressDialog(this);
		// waitingDialog.setMessage("正在登陆请稍后......");
		waitingDialog.setMessage(msg);
		waitingDialog.setIndeterminate(true);
		waitingDialog.setCancelable(true);
		return waitingDialog;
	}

	private AlertDialog.Builder getFailedDialog(String title, String msg,
			int dialogType) {

		if (null == failedDialog) {
			failedDialog = new AlertDialog.Builder(ConversationList.this)
					.setTitle(title).setCancelable(false).setMessage(msg);
			if (dialogType == TMmsSms2.ERROR_CODE_LOGIN_FAILURE) {
				failedDialog.setPositiveButton("重新登录",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {

								new LoginThread(TMmsSms2.LOGIN_TYPE_FIRST)
										.start();
							}
						}).setNegativeButton(R.string.giveup_first_sync, null);
			} else if (dialogType == TMmsSms2.ERROR_CODE_SYNC_FAILURE) {

				failedDialog.setPositiveButton(R.string.retry_first_sync,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								// 不应删除全部数据，只删除已同步到服务器的数据
								// MmsSmsDatabaseHelper.getInstance(mContext)
								// .wipeData();

								syncThread = new SyncThread(dialogShowHandler);
								syncThread.start();
							}
						}).setNegativeButton(R.string.giveup_first_sync, null);
			}

		}
		return failedDialog;
	}

	private void sync(int syncType) {

		if (syncType == TMmsSms2.SYNC_TYPE_FIRST) {

			getFirstSyncDialog(syncType).show();
		} else if (syncType == TMmsSms2.SYNC_TYPE_RESYNC) {
			// 不应删除全部数据，只删除已同步到服务器的数据
			// MmsSmsDatabaseHelper.getInstance(mContext).wipeData();

			syncThread = new SyncThread(dialogShowHandler);
			syncThread.start();

		}
	}

	private AlertDialog.Builder fristSyncDialog;

	private AlertDialog.Builder getFirstSyncDialog(int syncType) {

		if (null == fristSyncDialog) {
			fristSyncDialog = new AlertDialog.Builder(this);

			fristSyncDialog
					.setTitle(R.string.first_sync)
					.setMessage(R.string.first_sync_message)
					.setCancelable(false)
					.setPositiveButton(R.string.first_synce_confirm,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// 不应删除全部数据，只删除已同步到服务器的数据
									// MmsSmsDatabaseHelper.getInstance(mContext)
									// .wipeData();
									// getContentResolver().delete(
									// TThreads.CONTENT_URI, null, null);

									syncThread = new SyncThread(
											dialogShowHandler);
									syncThread.start();

								}
							})
					.setNegativeButton(R.string.first_sync_canel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();

								}
							});
		}
		return fristSyncDialog;

	}

	private int total;
	private Thread syncThread;

	private class SyncThread extends Thread {
		// add by chenqiang
		Handler mHandler;
		Message msg;
		int progress = 0;

		public SyncThread(Handler h) {
			mHandler = h;
			syncProgressDialog = new ProgressDialog(ConversationList.this);

		}

		@Override
		public void run() {
			Looper.prepare();

			try {
				// 删除本地已同步的短信彩信
				getContentResolver().delete(TSms.CONTENT_URI, "guid != -1",
						null);
				getContentResolver().delete(TMms.CONTENT_URI, "guid != -1",
						null);

				// todo: 请求同步联系人总数；
				total = SyncAction.getCountSMS(ConversationList.this,
						MmsConfig.getUserID());
				// 12.14 chenqiang
				if (MmsConfig.getSessionID() == null
						&& HttpCommunication.sessionID != null) {
					MmsConfig.setSessionID(HttpCommunication.sessionID
							.getValue());
				}

				// 登录结束，并获取总数，发送消息显示进度对话框
				// send to progress dialog to display the count
				// if (waitingDialog != null && waitingDialog.isShowing()) {
				// waitingDialog.dismiss();
				// }
				// showDialog(R.id.dialog_sync_progress);

				// syncProgressDialog = new
				// ProgressDialog(ConversationList.this);
				syncProgressDialog
						.setMessage(getString(R.string.waiting_for_sync));

				syncProgressDialog.setCancelable(true);
				syncProgressDialog.setButton("取消",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int i) {

								msg = mHandler.obtainMessage();
								msg.what = 3;
								mHandler.sendMessage(msg);
							}
						});
				syncProgressDialog
						.setOnCancelListener(new DialogInterface.OnCancelListener() {
							public void onCancel(DialogInterface dialog) {
								// 发送handler进行取消
								msg = mHandler.obtainMessage();
								msg.what = 3;
								mHandler.sendMessage(msg);
							}
						});

				syncProgressDialog.setMax(total); // set the contact count
				syncProgressDialog
						.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				syncProgressDialog.setTitle(R.string.first_sync);
				// syncProgressDialog.show();

				msg = mHandler.obtainMessage();
				msg.what = R.id.dialog_sync_progress;
				Bundle b = new Bundle();
				b.putInt("total", total);
				b.putInt("progress", progress);
				msg.setData(b);
				mHandler.sendMessage(msg);

				if (total > 0) {
					// todo: 从服务器下载已删除的信息
					int page2 = 0;
					List<SyncRecord> result2 = new ArrayList<SyncRecord>();
					do {
						page2++;
						try {
							result2 = SyncAction.recoverMessage(mContext,
									page2, null);
							for (SyncRecord i : result2) {
								SMSRecord s = (SMSRecord) i;
								MmsUtils.inserRemoteThreadInto(mContext, s,
										SyncState.SYNC_STATE_REMOTE_DELETE);
								progress++;
								msg = mHandler.obtainMessage();
								msg.what = R.id.dialog_sync_progress;
								b = new Bundle();
								b.putInt("total", total);
								b.putInt("progress", progress);
								msg.setData(b);
								mHandler.sendMessage(msg);
							}
						} catch (ClientProtocolException e) {
						} catch (ElementNotFound e) {
						} catch (IOException e) {
						}
					} while (result2.size() == 5);

					// begin to first sync!
					List<SyncRecord> result = new ArrayList<SyncRecord>();
					int page = 0;
					do {
						page++;
						result = SyncAction.firstSyncSMS(mContext, page,
								page == 1);
						for (SyncRecord i : result) {

							// SMSRecord s = (SMSRecord) i;
							SMSRecord s = new SMSRecord(i.getGuid(),
									i.getTag(), i.getData());
							MmsUtils.insertOneMessageInto(
									ConversationList.this, s,
									SyncState.SYNC_STATE_PRESENT);
							progress++;
							msg = mHandler.obtainMessage();
							msg.what = R.id.dialog_sync_progress;
							b = new Bundle();
							b.putInt("total", total);
							b.putInt("progress", progress);
							msg.setData(b);
							mHandler.sendMessage(msg);

						}
					} while (result.size() == 5);
					ConversationList.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							AlertDialog.Builder builder = new AlertDialog.Builder(
									ConversationList.this)
									.setTitle(getString(R.string.menu_about))
									.setPositiveButton(
											getString(android.R.string.ok),
											null)
									.setMessage(getString(R.string.about_text));
							builder.create().show();
						}
					});
				}

				// dialogShowHandler.sendEmptyMessage(0);
			} catch (Exception e) {
				// Log.d(TAG, e.getMessage());
				e.printStackTrace();
				MmsConfig.setFirstSync(true, ConversationList.this);
				// dialogShowHandler.sendEmptyMessage(2);
				msg = mHandler.obtainMessage();
				String errorMsg = e.getMessage();
				msg.what = R.id.dialog_sync_failed;
				Bundle bundle = new Bundle();
				bundle.putInt(TMmsSms2.KEY_ERROR_CODE,
						TMmsSms2.ERROR_CODE_SYNC_FAILURE);
				// bundle.putString(TMmsSms2.KEY_ERROR_MSG, errorMsg);
				bundle.putString(TMmsSms2.KEY_ERROR_MSG, "与服务器同步失败");
				msg.setData(bundle);
				mHandler.sendMessage(msg);
				// ContactsListActivity.this.finish();
			}

		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (waitingDialog != null) {
			waitingDialog.dismiss();
		}
	}
}
