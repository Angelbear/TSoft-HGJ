package cn.edu.tsinghua.hpc.tmms.syncaction;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.util.Log;
import cn.edu.tsinghua.hpc.google.tmms.InvalidHeaderValueException;
import cn.edu.tsinghua.hpc.google.tmms.MmsException;
import cn.edu.tsinghua.hpc.google.tmms.pdu.PduComposer;
import cn.edu.tsinghua.hpc.google.tmms.pdu.PduPersister;
import cn.edu.tsinghua.hpc.syncbroker.ElementNotFound;
import cn.edu.tsinghua.hpc.syncbroker.HttpCommunication;
import cn.edu.tsinghua.hpc.syncbroker.SMSRecord;
import cn.edu.tsinghua.hpc.syncbroker.SMSSubType;
import cn.edu.tsinghua.hpc.syncbroker.SMSType;
import cn.edu.tsinghua.hpc.syncbroker.ServerActionFailed;
import cn.edu.tsinghua.hpc.syncbroker.SyncCommand;
import cn.edu.tsinghua.hpc.syncbroker.SyncRequestBuilder;
import cn.edu.tsinghua.hpc.syncbroker.SyncResponseHandler;
import cn.edu.tsinghua.hpc.syncbroker.XMLTag;
import cn.edu.tsinghua.hpc.tmms.MmsConfig;
import cn.edu.tsinghua.hpc.tmms.ui.MessageUtils;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMmsSms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TSms;


public class TaskExecuter {

	public static SMSRecord getMMSRecordById(String localId, Context ctx) throws InvalidHeaderValueException {
		Cursor mCursor = ctx.getContentResolver().query(
				TMms.CONTENT_URI,
				new String[] { Mms.SUBJECT, Mms.DATE, Mms.MESSAGE_BOX,
						Mms.THREAD_ID, "guid" }, Mms._ID + " = ?",
				new String[] { localId }, null);
		if (mCursor == null || !mCursor.moveToFirst()) {
			if (mCursor != null) {
				try {
					mCursor.close();
				} catch (SQLiteException e) {
				}
				mCursor = null;
			}
			return null;
		}

		Log.d("Mms", "mms " + localId);
		SMSRecord sr = null;

		String data = null;

		if (mCursor.getInt(mCursor.getColumnIndex(Mms.MESSAGE_BOX)) == Mms.MESSAGE_BOX_INBOX) {
			Log.d("Mms", "inbox");
			Uri mms0data = Uri.withAppendedPath(
					Uri.withAppendedPath(TMms.AUTHORITY_URI, "data"),
					//Uri.parse("content://mms/data/"), 
					String.valueOf(localId));
			Cursor c = ctx.getContentResolver().query(mms0data,
					new String[] { "mms_id", "data" }, null, null, null);
			if (c != null && c.moveToNext()) {
				data = c.getString(c.getColumnIndex("data"));
				c.close();
			} else {
				return null;
			}

			sr = new SMSRecord(
					MessageUtils.getAddressByThreadId(ctx, mCursor
							.getLong(mCursor.getColumnIndex(Mms.THREAD_ID))),
					Const.localNumber, // to
					new Date(mCursor.getLong(mCursor.getColumnIndex(Mms.DATE))), // date
					SMSType.RECEIVE, // type
					SMSSubType.FRIEND, // subtype
					true,// read
					false, // secret
					mCursor.getString(mCursor.getColumnIndex(Mms.SUBJECT)),// subject
					data);
		} else if (mCursor.getInt(mCursor.getColumnIndex(Mms.MESSAGE_BOX)) == Mms.MESSAGE_BOX_OUTBOX
				|| mCursor.getInt(mCursor.getColumnIndex(Mms.MESSAGE_BOX)) == Mms.MESSAGE_BOX_SENT) {
			Log.d("Mms", "outbox");
			Uri mmsUri = Uri.withAppendedPath(TMms.CONTENT_URI, localId);
			PduPersister persister = PduPersister.getPduPersister(ctx);
			try {
				data = StringEncoder.encode(new PduComposer(ctx, persister
						.load(mmsUri)).make());
			} catch (MmsException e) {
				return null;
			}

			sr = new SMSRecord(
					Const.localNumber, // to
					MessageUtils.getAddressByThreadId(ctx, mCursor
							.getLong(mCursor.getColumnIndex(Mms.THREAD_ID))),
					new Date(mCursor.getLong(mCursor.getColumnIndex(Mms.DATE))), // date
					SMSType.SEND, // type
					SMSSubType.FRIEND, // subtype
					true,// read
					false, // secret
					mCursor.getString(mCursor.getColumnIndex(Mms.SUBJECT)),// subject
					data);
		}
		return sr;
	}

	public static SMSRecord getSMSRecordById(String localId, Context ctx) {

		Cursor mCursor = ctx.getContentResolver().query(
				TSms.CONTENT_URI,
				new String[] { Sms.ADDRESS, Sms.DATE, Sms.TYPE, Sms.BODY,
						"guid" }, Sms._ID + " = ?", new String[] { localId },
				null);

		if (mCursor == null || !mCursor.moveToFirst()) {
			if (mCursor != null) {
				try {
					mCursor.close();
				} catch (SQLiteException e) {
				}
				mCursor = null;
			}
			return null;
		}
//		Log.d("TaskExecuter","getSMSRecordById " + mCursor.getInt(mCursor.getColumnIndex(Sms.TYPE)));
		SMSRecord sr = null;
		if (mCursor.getInt(mCursor.getColumnIndex(Sms.TYPE)) == Sms.MESSAGE_TYPE_INBOX) {
			sr = new SMSRecord(
					mCursor.getString(mCursor.getColumnIndex(Sms.ADDRESS)), // from
					Const.localNumber, // to
					new Date(mCursor.getLong(mCursor.getColumnIndex(Sms.DATE))), // date
					SMSType.RECEIVE, // type
					SMSSubType.FRIEND, // subtype
					true,// read
					false, // secret
					mCursor.getString(mCursor.getColumnIndex(Sms.BODY))); // body

		} else if (mCursor.getInt(mCursor.getColumnIndex(Sms.TYPE)) == Sms.MESSAGE_TYPE_SENT
				|| mCursor.getInt(mCursor.getColumnIndex(Sms.TYPE)) == Sms.MESSAGE_TYPE_OUTBOX
				|| mCursor.getInt(mCursor.getColumnIndex(Sms.TYPE)) == Sms.MESSAGE_TYPE_DRAFT
				|| mCursor.getInt(mCursor.getColumnIndex(Sms.TYPE)) == Sms.MESSAGE_TYPE_QUEUED) {
			sr = new SMSRecord(
					Const.localNumber, // from
					mCursor.getString(mCursor.getColumnIndex(Sms.ADDRESS)), // to
					new Date(mCursor.getLong(mCursor.getColumnIndex(Sms.DATE))), // date
					SMSType.SEND, // type
					SMSSubType.FRIEND, // subtype
					true,// read
					false, // secret
					mCursor.getString(mCursor.getColumnIndex(Sms.BODY))); // body

		}

		if (!mCursor.isNull(mCursor.getColumnIndex("guid"))) {
			sr.setGuid(mCursor.getInt(mCursor.getColumnIndex("guid")));
		} else {
			sr.setGuid(-1);
		}
		mCursor.close();
		return sr;
	}

	public static int executeAddMmsTask(String localId, Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException, InvalidHeaderValueException {
		SyncCommand csc = null;
		HashMap<String, String> data = new HashMap<String, String>();
		SyncRequestBuilder srb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = null;
		HttpCommunication hc = new HttpCommunication();
		SyncResponseHandler srh = new SyncResponseHandler();
		String ack = null;
		int guid = -1;
		csc = SyncCommand.ADD;

		SMSRecord sr = getMMSRecordById(localId, ctx);
		if (sr == null)
			throw new ElementNotFound("Mms " + localId + " does not exist");

		data.put(XMLTag.DATA.name(), sr.toVSMS());
		xml = srb.buildXML(csc, data); // throws ElementNotFound
		try {
			ack = hc.postXML(Const.url, xml);
			guid = srh.handleAddACK(ack);
		} catch (ServerActionFailed e) {
		}
		return guid;
	}

	public static int executeAddSMSTask(String localId, Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException {
		SyncCommand csc = null;
		HashMap<String, String> data = new HashMap<String, String>();
		SyncRequestBuilder srb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = null;
		HttpCommunication hc = new HttpCommunication();
		SyncResponseHandler srh = new SyncResponseHandler();
		String ack = null;
		int guid = -1;
		csc = SyncCommand.ADD;

		SMSRecord sr = getSMSRecordById(localId, ctx);
		if (sr == null)
			return guid;

		data.put(XMLTag.DATA.name(), sr.toVSMS());
		xml = srb.buildXML(csc, data); // throws ElementNotFound
		try {
			ack = hc.postXML(Const.url, xml);
			guid = srh.handleAddACK(ack);
		} catch (ServerActionFailed e) {
		}
		return guid;
	}

	public static boolean executeUpdateSMSTask(String localId, Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException {
		if (true)
			return true;

		SyncCommand csc = null;
		HashMap<String, String> data = new HashMap<String, String>();
		SyncRequestBuilder srb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = null;
		HttpCommunication hc = new HttpCommunication();
		SyncResponseHandler srh = new SyncResponseHandler();
		String ack = null;
		csc = SyncCommand.UPDATE;

		SMSRecord sr = getSMSRecordById(localId, ctx);
		if (sr == null || sr.getGuid() == -1)
			return false;

		data.put(XMLTag.FILTER.name(), "guid=" + localId);
		data.put(XMLTag.DATA.name(), sr.toVSMS());
		xml = srb.buildXML(csc, data); // throws ElementNotFound
		try {
			ack = hc.postXML(Const.url, xml);
			return srh.handleUpdateACK(ack);
		} catch (ServerActionFailed e) {
			return false;
		}

	}

	public static boolean executeUpdateMMSTask(String localId, Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException, InvalidHeaderValueException {
		if (true)
			return true;

		SyncCommand csc = null;
		HashMap<String, String> data = new HashMap<String, String>();
		SyncRequestBuilder srb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = null;
		HttpCommunication hc = new HttpCommunication();
		SyncResponseHandler srh = new SyncResponseHandler();
		String ack = null;
		csc = SyncCommand.UPDATE;

		SMSRecord sr = getMMSRecordById(localId, ctx);
		if (sr == null || sr.getGuid() == -1)
			return false;

		data.put(XMLTag.FILTER.name(), "guid=" + localId);
		data.put(XMLTag.DATA.name(), sr.toVSMS());
		xml = srb.buildXML(csc, data); // throws ElementNotFound
		try {
			ack = hc.postXML(Const.url, xml);
			return srh.handleUpdateACK(ack);
		} catch (ServerActionFailed e) {
			return false;
		}

	}

	public static boolean executeAchieveTask(String guid, Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException {
		SyncCommand csc = SyncCommand.DEACTIVATE;
		HashMap<String, String> data = new HashMap<String, String>();
		/**
		 * XXX When final delete we record guid in task , for local id is
		 * already lost in UI
		 * 
		 */
		data.put(XMLTag.FILTER.name(), "guid=" + guid);
		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = csrb.buildXML(csc, data);

		HttpCommunication hc = new HttpCommunication();
		String ack;
		try {
			ack = hc.postXML(Const.url, xml);
			SyncResponseHandler csrh = new SyncResponseHandler();
			return csrh.handleMarkACK(ack);
		} catch (ServerActionFailed e) {
			return false;
		}

	}

	public static boolean executeDeleteTask(String guid, Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException {
		SyncCommand csc = SyncCommand.RECYCLE;
		HashMap<String, String> data = new HashMap<String, String>();
		/**
		 * XXX When final delete we record guid in task , for local id is
		 * already lost in UI
		 * 
		 */
		data.put(XMLTag.FILTER.name(), "guid=" + guid);
		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = csrb.buildXML(csc, data);

		HttpCommunication hc = new HttpCommunication();
		String ack;
		try {
			ack = hc.postXML(Const.url, xml);
			SyncResponseHandler csrh = new SyncResponseHandler();
			return csrh.handleMarkACK(ack);
		} catch (ServerActionFailed e) {
			return false;
		}

	}

	public static boolean executeFinalDelete(String guid, Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException {
		SyncCommand csc = SyncCommand.REMOVE;
		HashMap<String, String> data = new HashMap<String, String>();
		/**
		 * XXX When final delete we record guid in task , for local id is
		 * already lost in UI
		 * 
		 */
		data.put(XMLTag.FILTER.name(), "guid=" + guid);
		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = csrb.buildXML(csc, data);

		HttpCommunication hc = new HttpCommunication();
		String ack;
		try {
			ack = hc.postXML(Const.url, xml);
			SyncResponseHandler csrh = new SyncResponseHandler();
			return csrh.handleRemoveACK(ack);

		} catch (ServerActionFailed e) {
			return false;
		}
	}

	public static boolean executeRecoverTask(String guid, Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException {
		SyncCommand csc = SyncCommand.RECOVER;
		HashMap<String, String> data = new HashMap<String, String>();
		/**
		 * XXX localId is GUID here
		 * 
		 */
		data.put(XMLTag.FILTER.name(), "guid=" + guid);
		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, MmsConfig.getUserID());
		String xml = csrb.buildXML(csc, data);

		HttpCommunication hc = new HttpCommunication();
		String ack;
		try {
			ack = hc.postXML(Const.url, xml);
			SyncResponseHandler csrh = new SyncResponseHandler();
			return csrh.handleRemoveACK(ack);
		} catch (ServerActionFailed e) {
			return false;
		}

	}

}
