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

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import cn.edu.tsinghua.hpc.tmms.R;
import cn.edu.tsinghua.hpc.tmms.data.Conversation;

/**
 * The back-end data adapter for ConversationList.
 */
// TODO: This should be public class ConversationListAdapter extends
// ArrayAdapter<Conversation>
public class ConversationListAdapter extends CursorAdapter implements
		AbsListView.RecyclerListener {
	private static final String TAG = "ConversationListAdapter";
	private static final boolean LOCAL_LOGV = false;

	private final LayoutInflater mFactory;
	private OnContentChangedListener mOnContentChangedListener;


	private class UpdateListHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			onRefeshContent();
		}

	}

	private void onRefeshContent() {
		this.notifyDataSetChanged();
	}

	public UpdateListHandler handler = new UpdateListHandler();

	
	public ConversationListAdapter(Context context, Cursor cursor) {
		super(context, cursor, false /* auto-requery */);
		mFactory = LayoutInflater.from(context);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		if (!(view instanceof ConversationHeaderView)) {
			Log.e(TAG, "Unexpected bound view: " + view);
			return;
		}

		ConversationHeaderView headerView = (ConversationHeaderView) view;
		Conversation conv = Conversation.from(context, cursor);

		ConversationHeader ch = new ConversationHeader(context, conv);
		headerView.bind(context, ch);
	}

	public void onMovedToScrapHeap(View view) {
		ConversationHeaderView headerView = (ConversationHeaderView) view;
		headerView.unbind();
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		if (LOCAL_LOGV)
			Log.v(TAG, "inflating new view");
		return mFactory.inflate(R.layout.conversation_header, parent, false);
	}

	public interface OnContentChangedListener {
		void onContentChanged(ConversationListAdapter adapter);
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
}
