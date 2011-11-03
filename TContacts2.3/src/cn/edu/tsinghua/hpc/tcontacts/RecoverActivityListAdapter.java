package cn.edu.tsinghua.hpc.tcontacts;



import cn.edu.tsinghua.hpc.tcontacts.util.TContactsContract.TPhone;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.CursorAdapter;

public class RecoverActivityListAdapter extends CursorAdapter {
	private OnContentChangedListener mOnContentChangedListener;
	private final LayoutInflater mFactory;

	public RecoverActivityListAdapter(Context context, Cursor c,
			boolean autoRequery) {
		super(context, c, autoRequery);
		mFactory = LayoutInflater.from(context);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		CheckedTextView textView = (CheckedTextView) view;
		if (cursor != null) {
			String displayName = cursor.getString(cursor
					.getColumnIndex(Contacts.DISPLAY_NAME));
			
			//取得联系人的号码  
			String contactId = cursor.getString(cursor.getColumnIndex("_id"));
			ContentResolver cr = context.getContentResolver();  
            Cursor phone = cr.query(TPhone.CONTENT_URI,new String[]{"data1"}, "contact_id" + " = " + contactId,  null, null);     
  
            if (phone!=null && phone.getCount()>0){
            	
            	phone.moveToFirst();
            	int numberIndex = phone.getColumnIndex("data1");  
            	// Log.v("Recover Ativity List", number); 
            	String number = phone.getString(numberIndex);  
            	Log.v("Recover Ativity List", number);  
            	displayName = displayName+"\n"+number;  
            	
            	phone.close();  
            }
			textView.setText(displayName);
		}else{
			textView.setText(context.getText(android.R.string.unknownName));
		}
	}
	
	public interface OnContentChangedListener {
		void onContentChanged(
				RecoverActivityListAdapter recoverActivityListAdapter);
	}

	public void setOnContentChangedListener(OnContentChangedListener l) {
		mOnContentChangedListener = l;
	}

	@Override
	protected void onContentChanged() {
		if (this.getCursor() != null && !this.getCursor().isClosed()) {
			if (mOnContentChangedListener != null) {
				mOnContentChangedListener.onContentChanged(this);
			}
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mFactory.inflate(
				android.R.layout.simple_list_item_multiple_choice, parent,
				false);
	}
}
