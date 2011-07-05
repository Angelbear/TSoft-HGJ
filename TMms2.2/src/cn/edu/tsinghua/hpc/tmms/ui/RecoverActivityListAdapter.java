package cn.edu.tsinghua.hpc.tmms.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import cn.edu.tsinghua.hpc.google.tmms.MmsException;
import cn.edu.tsinghua.hpc.google.tmms.pdu.EncodedStringValue;
import cn.edu.tsinghua.hpc.google.tmms.pdu.PduPersister;
import cn.edu.tsinghua.hpc.tmms.R;
import cn.edu.tsinghua.hpc.tmms.data.Contact;
import cn.edu.tsinghua.hpc.tmms.ui.MessageListAdapter.ColumnsMap;

public class RecoverActivityListAdapter extends CursorAdapter {

	private final String TAG = "RecoverActivityListAdapter";

	private OnContentChangedListener mOnContentChangedListener;
	private final LayoutInflater mFactory;
	private Context mContext;

	public RecoverActivityListAdapter(Context context, Cursor c,
			boolean autoRequery) {
		super(context, c, autoRequery);
		mFactory = LayoutInflater.from(context);
		mContext = context;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		CheckedTextView textView = (CheckedTextView) view;
		// add by chenqiang
		String title;
		String body;
		String time;
		if (cursor != null) {

			// judge the type of the message
			String type = cursor.getString(cursor
					.getColumnIndex(MmsSms.TYPE_DISCRIMINATOR_COLUMN));

			String displayText = "";
			if ("sms".equals(type) || "mms".equals(type)) {
				// add by chenqiang
				MessageItem item = null;
				try {
					item = new MessageItem(mContext, type, cursor,
							new ColumnsMap(), null);
				} catch (MmsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (item != null) {
					title = item.mContact;
					body = item.mBody;
					if (body != null && body.length() > 14) {
						body = body.substring(0, 14) + "...";
					}
					if (body == null) {
						body = "此信息为一张图片没有任何文字";
					}
					Date date = new Date(cursor.getLong(cursor
							.getColumnIndex(Mms.DATE)));
					time = date.toLocaleString();
					displayText = title + "\n   " + body + "\n  " + time;
					textView.setText(displayText);
					textView.setTextSize((float) 16.0);
				}
			} else {
				// if can not get, then use address
				textView.setText(MessageUtils.getAddressByThreadId(mContext,
						cursor.getLong(cursor
								.getColumnIndex(Conversations.THREAD_ID))));
				textView.setTextSize((float) 16.0);

			}
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
		// //add by chenqiang
		// return mFactory.inflate(
		// R.layout.threetext_list_item_mutiple_choice, parent,
		// false);
	}
}
