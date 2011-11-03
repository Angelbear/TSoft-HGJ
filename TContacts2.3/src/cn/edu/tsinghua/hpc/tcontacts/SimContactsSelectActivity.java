
package cn.edu.tsinghua.hpc.tcontacts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import cn.edu.tsinghua.hpc.tcontacts.ui.EditContactActivity;

public class SimContactsSelectActivity extends ListActivity {
    private static final String TAG = "SimContactsSelectActivity";

    private static final boolean DBG = true;

    protected static final int QUERY_TOKEN = 0;

    protected static final int INSERT_TOKEN = 1;

    protected static final int UPDATE_TOKEN = 2;

    protected static final int DELETE_TOKEN = 3;

    static final int RAWCONTACT_ID_COLUMN_INDEX = 0;

    static final int RAWCONTACT_DISPLAY_NAME_COLUMN_INDEX = 1;

    static final int RAWCONTACT_DISPLAY_NAME_ALTERNATIVE = 2;

    static final int RAWCONTACT_GROUP_ID_INDEX = 3;

    static final int RAWCONTACT_ACCOUNT_TYPE = 3;

    static final int RAWCONTACT_SORT_KEY_PRIMARY_COLUMN_INDEX = 4;

    static final int RAWCONTACT_KEY_ALTERNATIVE_COLUMN_INDEX = 5;

    static final int CONTACTS_ID_COLUMN_INDEX = 0;

    static final int CONTACT_PHOTO_ID_COLUMN_INDEX = 3;

    static final int CONTACT_LINK_COLUMN_INDEX = 4;

    static final int CONTACT_SORT_KEY_PRIMARY_COLUMN_INDEX = 6;

    static final int CONTACT_SORT_KEY_ALTERNATIVE_COLUMN_INDEX = 7;

    static final int CONTACT_PHONETIC_NAME_COLUMN_INDEX = 8;

    private ProgressDialog mProgressDialog;

    String accountName;

    String accountType;

    private TextView mEmptyText;

    protected QueryHandler mQueryHandler;

    protected CursorAdapter mCursorAdapter;

    protected Cursor mCursor = null;

    protected int mInitialSelection = -1;

    private SelectAdapter mListAdapter;

    private CheckBox checkAll;

    private LinearLayout l;

    private TextView tv;

    private static final String DISPLAY_NAME = "display_name";

    private int mode;

    private Button doAction;

    private Button cancel;

    private ImageView mImageView;

    private final static int CHECK_ALL = 1;

    private final static int UNCHECK_ALL = 2;

    private final static int ITEM_CHECK = 3;

    private final static int ITEM_UNCHECK = 4;

    static HashMap<Integer, VALUE> IOMap = new HashMap<Integer, VALUE>();
    static HashMap<Integer, MARK> DMap = new HashMap<Integer, MARK>();

    class VALUE {
        public String name;

        public String number;

        public String email;

        public boolean checked;
    }
    
    class MARK {
        int raw_id;

        int contact_id;

        int sourceid;
        boolean checked;
    }

    static final String[] RAW_CONTACTS_COLUMN_NAMES = new String[] {
            RawContacts._ID, // 0
            RawContacts.CONTACT_ID, // 1
            RawContacts.ACCOUNT_TYPE, // 2
            RawContacts.SOURCE_ID, // 3
            DISPLAY_NAME  //4
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Intent Intent = getIntent();
        if (Intent != null) {
            accountName = Intent.getStringExtra("account_name");
            accountType = Intent.getStringExtra("account_type");
            mode = Intent.getIntExtra("mode", -1);
        }
        if (DBG)
            Log.d(TAG, "---> accountName = " + accountName);
        if (DBG)
            Log.d(TAG, "---> accountType = " + accountType);
        
        setContentView(R.layout.multiple_checkbox_main);
        mEmptyText = (TextView) findViewById(R.id.empty_list_textView);
        mImageView = (ImageView)  findViewById(R.id.empty_list_imageView);
        mQueryHandler = new QueryHandler(getContentResolver());
        l = (LinearLayout) findViewById(R.id.select_list_linearLayout);
        tv = (TextView) findViewById(R.id.select_list_all_text);
        checkAll = (CheckBox) findViewById(R.id.select_list_all_item_checkBox);
        doAction = (Button)findViewById(R.id.common_softkey_left_button);
        cancel = (Button)findViewById(R.id.common_softkey_right_button);
        IOHandler.obtainMessage(UNCHECK_ALL).sendToTarget();
        
        if(mode == 0){
            setTitle(R.string.import_from_sim);
            doAction.setText(R.string.doImport);
        }else if(mode == 1){
            setTitle(R.string.export_to_sim);
            doAction.setText(R.string.doExport);
        }else if (mode ==2){
            setTitle(R.string.menu_delContact);
            doAction.setText(R.string.deleteConfirmation_title);
        }
        doAction.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                importSelected();
            }
        });
        cancel.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        l.setOnClickListener(new OnClickListener() {
            Message msg = null;

            @Override
            public void onClick(View v) {
                if (!checkAll.isChecked()) {
                    msg = IOHandler.obtainMessage(CHECK_ALL);
                    msg.sendToTarget();
                    checkAll.setChecked(true);
                } else {
                    msg = IOHandler.obtainMessage(UNCHECK_ALL);
                    msg.sendToTarget();
                    checkAll.setChecked(false);
                }
            }
        });
    }

    Handler IOHandler = new Handler() {
        private int position;

        private VALUE v;
        private MARK m;

        private int Count = 0;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CHECK_ALL:
                    if (mCursor != null) {
                        position = 0;
                        Log.d(TAG, "*** mCursor count = "+ mCursor.getCount());
                        Log.d(TAG, "*** mCursor  = "+ mCursor);
                        while (mCursor.moveToPosition(position)) {
                            Log.d(TAG, "*** mCursor moveToNext = succes " + (position + 1));
                            String name = null;
                            String number = null;
                            String email = null;
                            switch (mode) {
                                case 0:
                                case 1:
                                    name = mCursor.getString(4);
                                    String _id = mCursor.getString(0);
                                    Cursor c = getContentResolver().query(Data.CONTENT_URI, new String[] {
                                            Data.DATA1, Data.MIMETYPE,
                                    }, "raw_contact_id == ?", new String[] {
                                        "" + _id
                                    }, null);
                                    Log.d(TAG, "----> onListItemClick raw_contact_id == ? = " + _id);
                                    if (c != null) {
                                        while(c.moveToNext()){
                                            Log.d(TAG, "----> c.getInt(1) = " + c.getString(1));
                                            if (c.getString(1).equals("vnd.android.cursor.item/name")) {
                                                name = c.getString(0);
                                            }else if(c.getString(1).equals("vnd.android.cursor.item/phone_v2")){
                                                number = c.getString(0);
                                            }else if(c.getString(1).equals("vnd.android.cursor.item/email_v2")){
                                                email = c.getString(0);
                                            }
                                        }
                                    }
                                    c.close();
                                    
                                    v = new VALUE();
                                    v.name = name;
                                    v.number = number;
                                    v.email = email;
                                    v.checked = true;
                                    IOMap.put(position, v);
                                    position++;
                                    break;
                                case 2:
                                    int raw_id = mCursor.getInt(0);
                                    int contact_id = mCursor.getInt(1);
                                    int sourceid = mCursor.getInt(3);
                                    m = new MARK();
                                    m.raw_id = raw_id;
                                    m.contact_id = contact_id;
                                    m.sourceid = sourceid;
                                    m.checked = true;
                                    DMap.put(position, m);
                                    position++;
                                    break;
                            }
                        }
                        Count  = position;
                        tv.setText(R.string.unSelect_all);
                        Toast.makeText(getBaseContext(), "have choosen "+ Count, Toast.LENGTH_SHORT).show();
                        checkAllItem(true);
                    }
                    break;
                case UNCHECK_ALL:
                    if (mCursor != null) {
                        position = 0;
                        switch (mode) {
                            case 0:
                            case 1:
                                while (mCursor.moveToPosition(position)) {
                                    if (IOMap.containsKey(position))
                                        IOMap.remove(position);
                                    position++;
                                }
                                break;
                            case 2:
                                while (mCursor.moveToPosition(position)) {
                                    if (DMap.containsKey(position))
                                        DMap.remove(position);
                                    position++;
                                }
                                break;
                        }
                    }
                    Count = 0;
                    tv.setText(R.string.select_all);
                    Toast.makeText(getBaseContext(), "have choosen "+ Count, Toast.LENGTH_SHORT).show();
                    checkAllItem(false);
                    break;
                case ITEM_CHECK:
                    switch (mode) {
                        case 0:
                        case 1:
                            position = msg.arg1;
                            v = (VALUE) msg.obj;
                            Log.d(TAG, "postion = " + position + " " + v);
                            IOMap.put(position, v);
                            break;
                        case 2:
                            position = msg.arg1;
                            m = (MARK) msg.obj;
                            Log.d(TAG, "postion = " + position + " " + m);
                            DMap.put(position, m);
                            break;
                    }
                    
                    Count ++;
                    Toast.makeText(getBaseContext(), "have choosen "+ Count, Toast.LENGTH_SHORT).show();
                    break;
                case ITEM_UNCHECK:
                    switch (mode) {
                        case 0:
                        case 1:
                            position = msg.arg1;
                            if (IOMap.containsKey(position))
                                IOMap.remove(position);
                            Log.d(TAG, "postion = " + position);
                            break;
                        case 2:
                            position = msg.arg1;
                            if (DMap.containsKey(position))
                                DMap.remove(position);
                            Log.d(TAG, "postion = " + position);
                            break;
                    }
                    Count --;
                    Toast.makeText(getBaseContext(), "have choosen "+ Count, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    protected void checkAllItem(boolean checked) {
        ListView listView = this.getListView();
        int count = listView.getChildCount();
        for (int i = 0; i < count; i++) {
            Log.d(TAG, "checkAll Item i = " + i);
            View view = listView.getChildAt(i);
            CheckBox cb = (CheckBox) view.findViewById(R.id.multiple_checkbox);
            cb.setChecked(checked);
        }
    }

    private void importSelected() {
        ImportAllSimContactsThread importAllSimContactsThread = null;
        ExportAllPhoneContactsThread exportAllPhoneContactsThread = null;
        DeleteAllPhoneContactsThread deleteAllPhoneContactsThread = null;
        mProgressDialog = new ProgressDialog(this);
        switch (mode) {
            case 0:
                mProgressDialog.setTitle("Importing ");
                importAllSimContactsThread = new ImportAllSimContactsThread();
                break;
            case 1:
                mProgressDialog.setTitle("Exporting ");
                exportAllPhoneContactsThread = new ExportAllPhoneContactsThread();
                break;
            case 2:
                mProgressDialog.setTitle("deleting ");
                deleteAllPhoneContactsThread = new DeleteAllPhoneContactsThread();
                break;
        }

        mProgressDialog.setMessage("please wait ...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setProgress(0);
        mProgressDialog.setCancelable(true);
        if (IOMap != null && (mode ==1 || mode == 0)) {
            int select = 0;
            Iterator iterator = IOMap.keySet().iterator();
            while (iterator.hasNext()) {
                Integer key = (Integer) iterator.next();
                Log.d(TAG, "item = " + IOMap.get(key).name + " " + IOMap.get(key).number + " "
                        + IOMap.get(key).checked);
                if (IOMap.get(key).checked)
                    select++;
            }
            Log.d(TAG, "---> select num = " + select);
            mProgressDialog.setMax(select);
        }
        if (DMap != null && mode ==2) {
            int select = 0;
            Iterator iterator = DMap.keySet().iterator();
            while (iterator.hasNext()) {
                Integer key = (Integer) iterator.next();
                if (DMap.get(key).checked)
                    select++;
            }
            Log.d(TAG, "---> select num = " + select);
            mProgressDialog.setMax(select);
        }
        mProgressDialog.show();

        switch (mode) {
            case 0:
                importAllSimContactsThread.start();
                break;
            case 1:
                exportAllPhoneContactsThread.start();
                break;
            case 2:
                deleteAllPhoneContactsThread.start();
                break;
        }

    }
    
    public class DeleteAllPhoneContactsThread extends Thread implements OnCancelListener,
            OnClickListener {
        private static final int PERSIST_TRIES = 3;

        boolean mCanceled = false;

        public DeleteAllPhoneContactsThread() {
            super("DeleteAllPhoneContactsThread");
        }

        @Override
        public void run() {
            try {
                Iterator iterator = DMap.keySet().iterator();
                Log.d(TAG, " DeleteAllPhoneContactsThread DMap = " + DMap);
                while (!mCanceled && iterator.hasNext()) {
                    Integer key = (Integer) iterator.next();
                    if (DMap.get(key).checked)
                        actuallyDeleteOnePhoneContact(DMap.get(key));
                    mProgressDialog.incrementProgressBy(1);
                }
                mProgressDialog.dismiss();
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void actuallyDeleteOnePhoneContact(MARK mark) {
            Log.d(TAG, "---> actuallyDeleteOnePhoneContact " + mark.raw_id + " " + mark.contact_id
                    + " " + mark.sourceid);
            // ContentValues values = new ContentValues();
            int raw_id = mark.raw_id;
            int contact_id = mark.contact_id;
            int sourceid = mark.sourceid;
            int tries = 0;
            final ContentResolver resolver = getContentResolver();
            final ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            while (tries++ < PERSIST_TRIES) {
                try {
                	
                    if(sourceid > 0){
                    	/*
                        if (!PhoneBookManageSim.updateAdnRecordsInEfByIndex(sourceid, "", "")) {
                            break;
                        }
                        if(PhoneBookManageSim.getSIMType().equals(PhoneBookManageSim.THREEG)
                                && ContactsListActivity.mAdn2EmailIndex.containsKey(sourceid) ){
                            int emailIndex = ContactsListActivity.mAdn2EmailIndex.get(sourceid);
                            if(!PhoneBookManageSim.updateAdnRecordsEmailInEfByIndex("", emailIndex, -1))
                                break;
                            else
                                ContactsListActivity.mAdn2EmailIndex.remove(sourceid);
                        }
                        */
                        if (DBG)
                            Log.d(TAG, " ---->  sourceid " + sourceid + "del ");
                        ContactsListActivity.simMap.put(sourceid - 1, 0);
                        ContentProviderOperation.Builder builder =
                            ContentProviderOperation.newDelete(RawContacts.CONTENT_URI);
                        builder.withSelection("_id == ?", new String[]{""+ raw_id});
                        operationList.add(builder.build());
                        resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                    }else{
                        ContactsListActivity.simMap.put(sourceid - 1, 0);
                        ContentProviderOperation.Builder builder =
                            ContentProviderOperation.newDelete(RawContacts.CONTENT_URI);
                        builder.withSelection("_id == ?", new String[]{""+ raw_id});
                        operationList.add(builder.build());
                        resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Problem persisting user edits", e);
                    break;

                } catch (OperationApplicationException e) {
                    Log.w(TAG, "Version consistency failed, re-parenting: " + e.toString());
                }
            }
        }

        public void onCancel(DialogInterface dialog) {
            mCanceled = true;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCanceled = true;
                mProgressDialog.dismiss();
            } else {
                Log.e(TAG, "Unknown button event has come: " + dialog.toString());
            }
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub

        }

    }

    public class ExportAllPhoneContactsThread extends Thread implements OnCancelListener,
            OnClickListener {
        private static final int PERSIST_TRIES = 3;

        boolean mCanceled = false;

        public ExportAllPhoneContactsThread() {
            super("ExportAllPhoneContactsThread");
        }

        @Override
        public void run() {
            try {
                Iterator iterator = IOMap.keySet().iterator();
                Log.d(TAG, " ExportAllPhoneContactsThread IOMap = " + IOMap);
                while (!mCanceled && iterator.hasNext()) {
                    Integer key = (Integer) iterator.next();
                    if (IOMap.get(key).checked)
                        actuallyExportOnePhoneContact(IOMap.get(key));
                    mProgressDialog.incrementProgressBy(1);
                }
                mProgressDialog.dismiss();
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void actuallyExportOnePhoneContact(VALUE value) {
            Log.d(TAG, "---> actuallyExportOnePhoneContact " + value.name + " " + value.number);
            // ContentValues values = new ContentValues();
            String name = value.name;
            String number = value.number;
            String email = value.email;
            int tries = 0;
            final ContentResolver resolver = getContentResolver();
            final ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            while (tries++ < PERSIST_TRIES) {
                try {
                    int index = -1;
                    int update = 0;
                    int raw = -1;
                    Iterator iterator = ContactsListActivity.simMap.keySet().iterator();
                    while (iterator.hasNext()) {
                        Integer key = (Integer) iterator.next();
                        int isHasSimContact = (int) ContactsListActivity.simMap.get(key);
                        if (DBG)
                            Log.d(TAG, "Hash map ----> iterator = " + key + " " + ContactsListActivity.simMap.get(key));
                        if (isHasSimContact == 0) {
                            if (DBG)
                                Log.d(TAG, " find a null space in SIM ---------------- index key =  "+ key);
                            index = key + 1;
                            break;
                        }
                    }
                    if (index == -1) {
                        if (DBG)
                            Log.d(TAG, " ---> sim is full");
                        Looper.prepare();
                        Toast.makeText(getBaseContext(), R.string.contactSaveErrorToastSimFull,
                                Toast.LENGTH_LONG).show();
                        mProgressDialog.dismiss();
                        finish();
                        Looper.loop();
                        Looper.myLooper().quit();
                        return;
                    }

                    if (EditContactActivity.updateSimContact(index, name, number)) {
                        Log.d(TAG, "**** ^.^ updateSimContact, index = " + index + ", name = "
                                + name + ", number = " + number);
                        /*
                        if(PhoneBookManageSim.getSIMType().equals(PhoneBookManageSim.THREEG)){
                            int emailIndex = -1;
                            int max = PhoneBookManageSim.getEmailRecordsSize();
                            if((ContactsListActivity.mAdn2EmailIndex.size() < max)
                                    && email != null && !email.equals("")){
                                for ( emailIndex = 1; emailIndex< PhoneBookManageSim.getEmailRecordsSize(); emailIndex++){
                                    Log.d(TAG, "emailIndex try = " + emailIndex   );
                                    if ( !ContactsListActivity.mAdn2EmailIndex.containsValue(emailIndex))
                                        break;
                                }
                                if(EditContactActivity.updateSimEmail(emailIndex, index, email)){
                                    if (DBG) Log.d(TAG, "updateSimEmail() success "   );
                                    if (DBG) Log.d(TAG, "index =  " + index   );
                                    if (DBG) Log.d(TAG, "emailIndex =  " + emailIndex   );
                                    ContactsListActivity.mAdn2EmailIndex.put(index, emailIndex);
                                }
                            }else if(email != null && !email.equals("")){
                                email = null;
                            }else{
                                email = null;
                            }
                        }*/
                        ContactsListActivity.simMap.put(index - 1, 1);
                    } else {
                        break;
                    }

                    ContentProviderOperation.Builder builder = ContentProviderOperation
                            .newInsert(RawContacts.CONTENT_URI);
                    String myGroupsId = null;

                    builder.withValue(RawContacts.ACCOUNT_NAME, "com.android.contact.sim");
                    builder.withValue(RawContacts.ACCOUNT_TYPE, "com.android.contact.sim");
                    builder.withValue(RawContacts.SOURCE_ID, index);
                    builder.withValue(RawContacts.AGGREGATION_MODE,
                            RawContacts.AGGREGATION_MODE_DEFAULT);
                    operationList.add(builder.build());

                    builder = ContentProviderOperation
                            .newInsert(android.provider.ContactsContract.Data.CONTENT_URI);

                    builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, 0);
                    builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
                    builder.withValue(StructuredName.DISPLAY_NAME, name);
                    operationList.add(builder.build());

                    builder = ContentProviderOperation
                            .newInsert(android.provider.ContactsContract.Data.CONTENT_URI);
                    builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
                    builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                    builder.withValue(Phone.TYPE, 1);
                    builder.withValue(Phone.NUMBER, number);
                    builder.withValue(Data.IS_PRIMARY, 1);
                    operationList.add(builder.build());
                    
                    if (email != null) {
                        builder = ContentProviderOperation.newInsert(android.provider.ContactsContract.Data.CONTENT_URI);
                        builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
                        builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                        builder.withValue(Email.DATA1, email);
                        operationList.add(builder.build());
                        
                    }
                    
                    resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                    break;

                } catch (RemoteException e) {
                    Log.e(TAG, "Problem persisting user edits", e);
                    break;

                } catch (OperationApplicationException e) {
                    Log.w(TAG, "Version consistency failed, re-parenting: " + e.toString());
                }
            }
        }

        public void onCancel(DialogInterface dialog) {
            mCanceled = true;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCanceled = true;
                mProgressDialog.dismiss();
            } else {
                Log.e(TAG, "Unknown button event has come: " + dialog.toString());
            }
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub

        }

    }

    private class ImportAllSimContactsThread extends Thread implements OnCancelListener,
            OnClickListener {

        boolean mCanceled = false;

        public ImportAllSimContactsThread() {
            super("ImportAllSimContactsThread");
        }

        @Override
        public void run() {
            try {
                Iterator iterator = IOMap.keySet().iterator();
                while (!mCanceled && iterator.hasNext()) {
                    Integer key = (Integer) iterator.next();
                    if (IOMap.get(key).checked)
                        actuallyImportOneSimContact(IOMap.get(key));
                    mProgressDialog.incrementProgressBy(1);
                }
                mProgressDialog.dismiss();
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void actuallyImportOneSimContact(VALUE value) {
            Log.d(TAG, "---> value = " + value.name + " " + value.number + " " + value.email);
            String name = value.name;
            String number = value.number;
            String email = value.email;
            ContentResolver resolver = getContentResolver();
            final ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newInsert(RawContacts.CONTENT_URI);
            String myGroupsId = null;

            builder.withValue(RawContacts.ACCOUNT_NAME, accountName);
            builder.withValue(RawContacts.ACCOUNT_TYPE, accountType);
            builder.withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DEFAULT);
            operationList.add(builder.build());

            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, 0);
            builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            builder.withValue(StructuredName.DISPLAY_NAME, name);
            operationList.add(builder.build());

            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
            builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            builder.withValue(Phone.TYPE, 1);
            builder.withValue(Phone.NUMBER, number);
            builder.withValue(Data.IS_PRIMARY, 1);
            operationList.add(builder.build());
            if (email != null && !email.equals("")) {
                if (email.endsWith(","))
                    email = email.substring(0, email.length() - 1);
                builder = ContentProviderOperation.newInsert(android.provider.ContactsContract.Data.CONTENT_URI);
                builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
                builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                builder.withValue(Email.DATA1, email);
                operationList.add(builder.build());
            }
            try {
                resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void onCancel(DialogInterface dialog) {
            mCanceled = true;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCanceled = true;
                mProgressDialog.dismiss();
            } else {
                Log.e(TAG, "Unknown button event has come: " + dialog.toString());
            }
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IOMap.clear();
        DMap.clear();
        query();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IOMap.clear();
        DMap.clear();
        if (mCursor != null) {
            mCursor.close();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCursor != null) {
            mCursor.deactivate();
        }
    }

    protected Uri resolveIntent() {
        Intent intent = getIntent();
        switch (mode) {
            case 0:
            case 1:
            case 2:
                if (intent.getData() == null) {
                    intent.setData(RawContacts.CONTENT_URI);
                }
                break;
            default:
                intent.setData(null);
        }
        return intent.getData();
    }

    private class QueryHandler extends AsyncQueryHandler {

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            if (DBG)
                log("onQueryComplete: cursor.count=" + c.getCount());
            mCursor = c;
            setAdapter();
            displayProgress(false);
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            if (DBG)
                log("onInsertComplete: requery");
            reQuery();
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            if (DBG)
                log("onUpdateComplete: requery");
            reQuery();
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            if (DBG)
                log("onDeleteComplete: requery");
            reQuery();
        }
    }

    private void reQuery() {
        query();
    }

    private void query() {
        Uri uri = resolveIntent();
        if (DBG)
            log("query: starting an async query");
        switch (mode) {
            case 0: // import from SIM
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri, RAW_CONTACTS_COLUMN_NAMES,
                        "account_type == ? AND deleted != ?", 
                        new String[] { "com.android.contact.sim","1" }, null);
                break;
            case 1: // export to SIM
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri, RAW_CONTACTS_COLUMN_NAMES,
                        "account_type == ? AND deleted != ?", 
                        new String[] { "com.android.contact.phone","1" }, null);
                break;
            case 2: // delete contacts
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri, RAW_CONTACTS_COLUMN_NAMES,
                        "deleted != ?", 
                        new String[] { "1" }, null);
                break;
        }
        displayProgress(true);
    }

    private void displayProgress(boolean flag) {
        if (DBG)
            log("displayProgress: " + flag);
        mEmptyText.setText(flag ? R.string.simContacts_emptyLoading : R.string.simContacts_empty);
        mImageView.setVisibility(flag ? View.GONE : View.VISIBLE);
        getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                flag ? Window.PROGRESS_VISIBILITY_ON : Window.PROGRESS_VISIBILITY_OFF);
    }

    private void setAdapter() {
        if (mListAdapter == null) {
            mListAdapter = new SelectAdapter(this, R.layout.multiple_checkbox_main_row, mCursor);
            setListAdapter(mListAdapter);
        } else {
            mCursorAdapter.changeCursor(mCursor);
        }
    }

    protected void log(String msg) {
        Log.d(TAG, " " + msg);
    }

    private class SelectAdapter extends ResourceCursorAdapter {
        public SelectAdapter(Context context, int layout, Cursor c) {
            super(context, layout, c);
        }

        @Override
        public View newView(Context context, Cursor cur, ViewGroup parent) {
            LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            return li.inflate(R.layout.multiple_checkbox_main_row, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tv1 = (TextView) view.findViewById(R.id.multiple_title);
            TextView tv2 = (TextView) view.findViewById(R.id.multiple_summary);
            CheckBox cb = (CheckBox) view.findViewById(R.id.multiple_checkbox);
            switch (mode) {
                case 0:
                case 1:
                    tv1.setText("+ ");
                    tv1.append(cursor.getString(4));
                    if (IOMap.get(cursor.getPosition()) != null
                            && IOMap.get(cursor.getPosition()).checked)
                        cb.setChecked(true);
                    else
                        cb.setChecked(false);
                    break;
                case 2:
                    tv1.setText(" - ");
                    tv1.append(cursor.getString(4));
                    if(cursor.getInt(3) > 0){
                        tv1.append(" (SIM)");
                    }
                    if (DMap.get(cursor.getPosition()) != null
                            && DMap.get(cursor.getPosition()).checked)
                        cb.setChecked(true);
                    else
                        cb.setChecked(false);
                    break;
            }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        CheckBox cb = (CheckBox) v.findViewById(R.id.multiple_checkbox);
        if (mCursor == null)
            return;
        String name = null;
        String number = null;
        String email = null;
        switch (mode) {
            case 0:
            case 1:
                name = mCursor.getString(4);
                String _id = mCursor.getString(0);
                Cursor c = getContentResolver().query(Data.CONTENT_URI, new String[] {
                        Data.DATA1, Data.MIMETYPE,
                }, "raw_contact_id == ?", new String[] {
                    "" + _id
                }, null);
                Log.d(TAG, "----> onListItemClick raw_contact_id == ? = " + _id);
                if (c != null) {
                    while(c.moveToNext()){
                        Log.d(TAG, "----> c.getInt(1) = " + c.getString(1));
                        if (c.getString(1).equals("vnd.android.cursor.item/name")) {
                            name = c.getString(0);
                        }else if(c.getString(1).equals("vnd.android.cursor.item/phone_v2")){
                            number = c.getString(0);
                        }else if(c.getString(1).equals("vnd.android.cursor.item/email_v2")){
                            email = c.getString(0);
                        }
                    }
                }
                c.close();
                
                VALUE value = new VALUE();
                value.name = name;
                value.number = number;
                value.email = email;
                Message msg = null;
                if (cb.isChecked()) {
                    cb.setChecked(false);
                    value.checked = false;
                    msg = IOHandler.obtainMessage(ITEM_UNCHECK, position, 0, value);
                    msg.sendToTarget();
                } else {
                    cb.setChecked(true);
                    value.checked = true;
                    msg = IOHandler.obtainMessage(ITEM_CHECK, position, 0, value);
                    msg.sendToTarget();
                }
                Log.d(TAG, "----> onListItemClick name = " + name);
                Log.d(TAG, "----> onListItemClick number = " + number);
                Log.d(TAG, "----> onListItemClick email = " + email);
                break;
            case 2:
                int raw_id = mCursor.getInt(0);
                int contact_id = mCursor.getInt(1);
                int sourceid = mCursor.getInt(3);
                MARK mark = new MARK();
                mark.raw_id = raw_id;
                mark.contact_id = contact_id;
                mark.sourceid = sourceid;
                msg = null;
                if (cb.isChecked()) {
                    cb.setChecked(false);
                    mark.checked = false;
                    msg = IOHandler.obtainMessage(ITEM_UNCHECK, position, 0, mark);
                    msg.sendToTarget();
                } else {
                    cb.setChecked(true);
                    mark.checked = true;
                    msg = IOHandler.obtainMessage(ITEM_CHECK, position, 0, mark);
                    msg.sendToTarget();
                }
                break;
        }

    }
}
