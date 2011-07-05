package cn.edu.tsinghua.hpc.tmms.syncaction;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.ClientProtocolException;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.Telephony.Sms;
import cn.edu.tsinghua.hpc.syncbroker.ElementNotFound;
import cn.edu.tsinghua.hpc.syncbroker.HttpCommunication;
import cn.edu.tsinghua.hpc.syncbroker.ServerActionFailed;
import cn.edu.tsinghua.hpc.syncbroker.SyncCommand;
import cn.edu.tsinghua.hpc.syncbroker.SyncRecord;
import cn.edu.tsinghua.hpc.syncbroker.SyncRequestBuilder;
import cn.edu.tsinghua.hpc.syncbroker.SyncResponseHandler;
import cn.edu.tsinghua.hpc.syncbroker.XMLTag;
import cn.edu.tsinghua.hpc.tmms.MmsConfig;

public class SyncAction {

	public static int getCountSMS(Context ctx, String uid) throws ElementNotFound, ClientProtocolException,
		ServerActionFailed, IOException {
		SyncCommand csc = null;
		HashMap<String, String> data = new HashMap<String, String>();
		SyncRequestBuilder srb = new SyncRequestBuilder(ctx, uid);
		String xml = null;
		HttpCommunication hc = new HttpCommunication();
		SyncResponseHandler srh = new SyncResponseHandler();
		String ack = null;
		csc = SyncCommand.GETCOUNT;
		data.clear();
		// XXX
		data.put(XMLTag.FILTER.name(), "tag=DELETED|CACHED"); // tag=ARCHIVED|CACHED
		xml = srb.buildXML(csc, data);
		ack = hc.postXML(Const.url, xml);
		return srh.handleGetCountACK(ack);
	}
	
	public static int getCountSMS(Context ctx) throws ElementNotFound, ClientProtocolException,
		ServerActionFailed, IOException {
		return getCountSMS(ctx, MmsConfig.getUserID());
	}
	
	public static List<SyncRecord> firstSyncSMS(Context ctx, int page,
			boolean window) throws ElementNotFound, ClientProtocolException,
			IOException {
		SyncCommand csc = null;
		HashMap<String, String> data = new HashMap<String, String>();
		SyncRequestBuilder srb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = null;
		HttpCommunication hc = new HttpCommunication();
		SyncResponseHandler srh = new SyncResponseHandler();
		String ack = null;
		csc = SyncCommand.FIRSTSYNC;
		data.clear();
		// XXX
		if (window) {
			data.put(XMLTag.FILTER.name(), "page=" + page + "&count=5&window="
					+ MmsConfig.maxMessageNum); // count=n&page=m&tag=x&keywords
		} else {
			data.put(XMLTag.FILTER.name(), "page=" + page + "&count=5"); // count=n&page=m&tag=x&keywords
		}
		xml = srb.buildXML(csc, data);
		try {
			ack = hc.postXML(Const.url, xml);
			return srh.handleSearchACK(ack);
		} catch (ServerActionFailed e) {
			return null;
		}
	}
	
	public static List<SyncRecord> retriveDeletedThread(Context ctx, int page,
			int count) throws ElementNotFound, ClientProtocolException,
			IOException {
		SyncCommand csc = null;
		HashMap<String, String> data = new HashMap<String, String>();
		SyncRequestBuilder srb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = null;
		HttpCommunication hc = new HttpCommunication();
		SyncResponseHandler srh = new SyncResponseHandler();
		String ack = null;
		csc = SyncCommand.SEARCH;
		data.clear();

		data.put(XMLTag.FILTER.name(), "delitem=true&page="
				+ String.valueOf(page) + "&count=" + String.valueOf(count));
		xml = srb.buildXML(csc, data);
		try {
			ack = hc.postXML(Const.url, xml);
			return srh.handleSearchACK(ack);
		} catch (ServerActionFailed e) {
			return null;
		}

	}

	public static List<SyncRecord> recoverMessage(Context ctx, int page,
			String partner) throws ElementNotFound, ClientProtocolException,
			IOException {
		SyncCommand csc = null;
		HashMap<String, String> data = new HashMap<String, String>();
		SyncRequestBuilder srb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = null;
		HttpCommunication hc = new HttpCommunication();
		SyncResponseHandler srh = new SyncResponseHandler();
		String ack = null;
		csc = SyncCommand.SEARCH;
		data.clear();
		if (partner != null) {
			data.put(XMLTag.FILTER.name(), "page=" + page
					+ "&&count=5&&partner=" + partner + "&&tag=DELETED"); // count=n&page=m&tag=x&keywords
		} else {
			data.put(XMLTag.FILTER.name(), "page=" + page
					+ "&&count=5&&tag=DELETED"); // count=n&page=m&tag=x&keywords
		}
		xml = srb.buildXML(csc, data);
		try {
			ack = hc.postXML(Const.url, xml);
			return srh.handleSearchACK(ack);
		} catch (ServerActionFailed e) {
			return null;
		}
	}

	public static List<SyncRecord> recoverSMS(Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException {

		SyncCommand csc = null;
		HashMap<String, String> data = new HashMap<String, String>();
		SyncRequestBuilder srb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = null;
		HttpCommunication hc = new HttpCommunication();
		SyncResponseHandler srh = new SyncResponseHandler();
		String ack = null;
		csc = SyncCommand.SEARCH;
		data.clear();
		data.put(XMLTag.FILTER.name(), "tag=DELETED"); // count=n&page=m&tag=x&keywords
		xml = srb.buildXML(csc, data);
		try {
			ack = hc.postXML(Const.url, xml);
			return srh.handleSearchACK(ack);
		} catch (ServerActionFailed e) {
			return null;
		}
	}

	public static List<SyncRecord> retriveAchivedThread(Context ctx, int page,
			int count) throws ElementNotFound, ClientProtocolException,
			IOException {
		SyncCommand csc = null;
		HashMap<String, String> data = new HashMap<String, String>();
		SyncRequestBuilder srb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = null;
		HttpCommunication hc = new HttpCommunication();
		SyncResponseHandler srh = new SyncResponseHandler();
		String ack = null;
		csc = SyncCommand.SEARCH;
		data.clear();

		data.put(XMLTag.FILTER.name(), "newitem=true&page="
				+ String.valueOf(page) + "&count=" + String.valueOf(count));
		xml = srb.buildXML(csc, data);
		try {
			ack = hc.postXML(Const.url, xml);
			return srh.handleSearchACK(ack);
		} catch (ServerActionFailed e) {
			return null;
		}

	}

	public static List<SyncRecord> searchArchievedSMS(Context ctx, int page,
			int count, String queryString) throws ElementNotFound,
			ClientProtocolException, IOException {
		SyncCommand csc = null;
		HashMap<String, String> data = new HashMap<String, String>();
		SyncRequestBuilder srb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = null;
		HttpCommunication hc = new HttpCommunication();
		SyncResponseHandler srh = new SyncResponseHandler();
		String ack = null;
		csc = SyncCommand.SEARCH;
		data.clear();
		if (queryString == null) {
			data.put(XMLTag.FILTER.name(), "tag=ARCHIVED&page="
					+ String.valueOf(page) + "&count=" + String.valueOf(count));
		} else {
			data.put(XMLTag.FILTER.name(), "tag=ARCHIVED&" + queryString
					+ "&page=" + String.valueOf(page) + "&count="
					+ String.valueOf(count));
		}
		xml = srb.buildXML(csc, data);
		try {
			ack = hc.postXML(Const.url, xml);
			return srh.handleSearchACK(ack);
		} catch (ServerActionFailed e) {
			return null;
		}

	}

	public static List<SyncRecord> retriveArchievedSMS(Context ctx, int page,
			int count, String queryString) throws ElementNotFound,
			ClientProtocolException, IOException {
		SyncCommand csc = null;
		HashMap<String, String> data = new HashMap<String, String>();
		SyncRequestBuilder srb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = null;
		HttpCommunication hc = new HttpCommunication();
		SyncResponseHandler srh = new SyncResponseHandler();
		String ack = null;
		csc = SyncCommand.SEARCH;
		data.clear();
		if (queryString == null) {
			data.put(XMLTag.FILTER.name(), "tag=ARCHIVED&page="
					+ String.valueOf(page) + "&count=" + String.valueOf(count));
		} else {
			data.put(XMLTag.FILTER.name(), "tag=ARCHIVED&partner="
					+ queryString + "&page=" + String.valueOf(page) + "&count="
					+ String.valueOf(count));
		}
		xml = srb.buildXML(csc, data);
		try {
			ack = hc.postXML(Const.url, xml);
			return srh.handleSearchACK(ack);
		} catch (ServerActionFailed e) {
			return null;
		}

	}

	public static int getLocalIdByUrl(Uri uri, Context ctx) {
		Cursor mCursor = ctx.getContentResolver().query(uri,
				new String[] { Sms._ID }, null, null, null);
		if (mCursor == null || !mCursor.moveToFirst()) {
			if (mCursor != null) {
				try {
					mCursor.close();
				} catch (SQLiteException e) {
				}
				mCursor = null;
			}
		}
		return mCursor.getInt(mCursor.getColumnIndex(Sms._ID));
	}

	public static boolean finalDeleteThread(Context ctx, String partener) {
		SyncCommand csc = SyncCommand.REMOVE;
		HashMap<String, String> data = new HashMap<String, String>();
		data.put(XMLTag.FILTER.name(), "partner=" + partener);
		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml;
		try {
			xml = csrb.buildXML(csc, data);
		} catch (ElementNotFound e1) {
			return false;
		}

		HttpCommunication hc = new HttpCommunication();
		String ack;

		try {
			ack = hc.postXML(Const.url, xml);
			SyncResponseHandler csrh = new SyncResponseHandler();
			return csrh.handleMarkACK(ack);
		} catch (ClientProtocolException e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (ServerActionFailed e) {
			return false;
		}

	}

	public static boolean deleteThread(Context ctx, String partener) {
		SyncCommand csc = SyncCommand.RECYCLE;
		HashMap<String, String> data = new HashMap<String, String>();
		data.put(XMLTag.FILTER.name(), "partner=" + partener);
		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml;
		try {
			xml = csrb.buildXML(csc, data);
		} catch (ElementNotFound e1) {
			return false;
		}
		HttpCommunication hc = new HttpCommunication();
		String ack;
		try {
			ack = hc.postXML(Const.url, xml);
			SyncResponseHandler csrh = new SyncResponseHandler();
			return csrh.handleMarkACK(ack);
		} catch (ClientProtocolException e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (ServerActionFailed e) {
			return false;
		}

	}

}
