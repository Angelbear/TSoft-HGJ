package cn.edu.tsinghua.hpc.tcontacts.syncaction;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.util.Log;
import cn.edu.tsinghua.hpc.syncbroker.ElementNotFound;
import cn.edu.tsinghua.hpc.syncbroker.HttpCommunication;
import cn.edu.tsinghua.hpc.syncbroker.ServerActionFailed;
import cn.edu.tsinghua.hpc.syncbroker.SyncCommand;
import cn.edu.tsinghua.hpc.syncbroker.SyncRequestBuilder;
import cn.edu.tsinghua.hpc.syncbroker.SyncResponseHandler;
import cn.edu.tsinghua.hpc.syncbroker.XMLTag;
import cn.edu.tsinghua.hpc.tcontacts.Preferences;
import cn.edu.tsinghua.hpc.tcontacts.pim.VCardComposer;

public class TaskExecuter {
	private static final String TAG = "TaskExecuter: ";
	public static final Object mutex = new Object();

	public static boolean executeRecoverTask(String guid, Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException {
		SyncCommand csc = SyncCommand.RECOVER;
		HashMap<String, String> vcardData = new HashMap<String, String>();
		/**
		 * XXX When final delete we record guid in task , for local id is
		 * already lost in UI
		 * 
		 */
		vcardData.put(XMLTag.FILTER.name(), "guid=" + guid);
		
		String uid = Preferences.getUid(ctx); //added by Boern
		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, uid);
		
//		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, null);
		String xml = csrb.buildXML(csc, vcardData);
//		Log.v(TAG+"Sync to Recover :", xml);
		
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

	public static int executeAddTask(String localId, Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException {

		SyncCommand csc = SyncCommand.ADD;
		HashMap<String, String> vcardData = new HashMap<String, String>();
		VCardComposer vcardImpl = new VCardComposer(ctx);
		vcardImpl.init();

		String vcardString = vcardImpl.exportOneContactData(localId);
		vcardData.put(XMLTag.DATA.name(), vcardString);

		String uid = Preferences.getUid(ctx); //added by Boern
		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, uid);
		String xml = csrb.buildXML(csc, vcardData);
//		Log.v(TAG+"Sync to add requst :", xml);

		HttpCommunication hc = new HttpCommunication();
		String ack;
		int guid = -1;
		try {
			ack = hc.postXML(Const.url, xml);
//			Log.v(TAG+"Sync to add response :", ack);
			SyncResponseHandler csrh = new SyncResponseHandler();
			guid = csrh.handleAddACK(ack);
		} catch (ServerActionFailed e) {
		}
		return guid;

	}

	public static boolean executeUpdateTask(String localId, String guid,
			Context ctx) throws ElementNotFound, ClientProtocolException,
			IOException {
		SyncCommand csc = SyncCommand.UPDATE;
		HashMap<String, String> vcardData = new HashMap<String, String>();
		VCardComposer vcardImpl = new VCardComposer(ctx);
		vcardImpl.init();
		if (guid != null) {
			vcardData.put(XMLTag.GUID.name(), guid);
			vcardData.put(XMLTag.DATA.name(), vcardImpl
					.exportOneContactData(localId));

			String uid = Preferences.getUid(ctx); //added by Boern
			SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, uid);
//			SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, null);
			String xml = csrb.buildXML(csc, vcardData);
			
//			Log.v(TAG+"Sync to update :", xml);

			HttpCommunication hc = new HttpCommunication();
			String ack;
			try {
				ack = hc.postXML(Const.url, xml);
				SyncResponseHandler csrh = new SyncResponseHandler();
				return csrh.handleUpdateACK(ack);
			} catch (ServerActionFailed e) {
				return false;
			}

		}
		return false;

	}

	public static boolean executeAchiveTask(String localId, Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException {
		SyncCommand csc = SyncCommand.DEACTIVATE;
		HashMap<String, String> vcardData = new HashMap<String, String>();
		/**
		 * XXX When achive we record guid in task , for local id is already lost
		 * in UI
		 * 
		 */
		vcardData.put(XMLTag.FILTER.name(), "guid=" + localId);
		String uid = Preferences.getUid(ctx); //added by Boern
		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, uid);
		String xml = csrb.buildXML(csc, vcardData);
		
//		Log.v(TAG+"Sync to achive :", xml);

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

	public static boolean executeDeleteTask(String localId, Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException {
		SyncCommand csc = SyncCommand.RECYCLE;
		HashMap<String, String> vcardData = new HashMap<String, String>();
		/**
		 * XXX When delete we record guid in task , for local id is already lost
		 * in UI
		 * 
		 */
		vcardData.put(XMLTag.FILTER.name(), "guid=" + localId);
		
		String uid = Preferences.getUid(ctx); //added by Boern
		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, uid);
//		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, null);
		String xml = csrb.buildXML(csc, vcardData);

//		Log.v(TAG+"Sync to delete :", xml);
		
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

	public static boolean executeFinalDelete(String localId, Context ctx)
			throws ElementNotFound, ClientProtocolException, IOException {
		SyncCommand csc = SyncCommand.REMOVE;
		HashMap<String, String> vcardData = new HashMap<String, String>();
		/**
		 * XXX When final delete we record guid in task , for local id is
		 * already lost in UI
		 * 
		 */
		vcardData.put(XMLTag.FILTER.name(), "guid=" + localId);
		
		String uid = Preferences.getUid(ctx); //added by Boern
		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, uid);
//		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, null);
		String xml = csrb.buildXML(csc, vcardData);

//		Log.v(TAG+"Sync to remove :", xml);
		
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
