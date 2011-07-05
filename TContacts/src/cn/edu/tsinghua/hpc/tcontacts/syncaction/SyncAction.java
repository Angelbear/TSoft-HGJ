package cn.edu.tsinghua.hpc.tcontacts.syncaction;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.client.ClientProtocolException;

import android.R.integer;
import android.content.Context;
import android.util.Log;
import cn.edu.tsinghua.hpc.syncbroker.ElementNotFound;
import cn.edu.tsinghua.hpc.syncbroker.HttpCommunication;
import cn.edu.tsinghua.hpc.syncbroker.ServerActionFailed;
import cn.edu.tsinghua.hpc.syncbroker.SyncCommand;
import cn.edu.tsinghua.hpc.syncbroker.SyncRecord;
import cn.edu.tsinghua.hpc.syncbroker.SyncRequestBuilder;
import cn.edu.tsinghua.hpc.syncbroker.SyncResponseHandler;
import cn.edu.tsinghua.hpc.syncbroker.XMLTag;
import cn.edu.tsinghua.hpc.tcontacts.Preferences;
import cn.edu.tsinghua.hpc.tcontacts.pim.VCardComposer;

public class SyncAction {

	private static String TAG = "SyncAction: ";

	public static ExecutorService SyncActionPool = Executors
			.newSingleThreadExecutor();

	public static boolean updateContact(int guid, String vcard, Context ctx) {
		SyncCommand csc = SyncCommand.UPDATE;
		HashMap<String, String> vcardData = new HashMap<String, String>();
		VCardComposer vcardImpl = new VCardComposer(ctx);
		vcardImpl.init();

		vcardData.put(XMLTag.GUID.name(), String.valueOf(guid));
		vcardData.put(XMLTag.DATA.name(), vcard);

		String uid = Preferences.getUid(ctx); // added by Boern
		SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, uid);
		// SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, null);
		String xml;
		try {
			xml = csrb.buildXML(csc, vcardData);
		} catch (ElementNotFound e1) {
			// TODO Auto-generated catch block
			return false;
		}

		HttpCommunication hc = new HttpCommunication();
		String ack;
		try {

			ack = hc.postXML(Const.url, xml);
			SyncResponseHandler csrh = new SyncResponseHandler();
			return csrh.handleUpdateACK(ack);
		} catch (ServerActionFailed e) {
			return false;
		} catch (ClientProtocolException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}

	private static class RetriveAchivedContactsCallable implements Callable {
		private Context ctx;
		private int page;
		private int count;
		private String queryString;
		private String uid;

		public RetriveAchivedContactsCallable(Context context, int page,
				int count, String queryString) {
			ctx = context;
			this.page = page;
			this.count = count;
			this.queryString = queryString;
			// this.uid = uid;
		}

		public List<SyncRecord> call() throws Exception {
			SyncCommand csc = SyncCommand.SEARCH;
			HashMap<String, String> data = new HashMap<String, String>();
			if (queryString == null) {
				data.put(XMLTag.FILTER.name(), "tag=ARCHIVED&page="
						+ String.valueOf(page) + "&count="
						+ String.valueOf(count));
			} else {
				data.put(XMLTag.FILTER.name(), "tag=ARCHIVED&" + queryString
						+ "&page=" + String.valueOf(page) + "&count="
						+ String.valueOf(count));
			}
			// SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, uid);

			this.uid = Preferences.getUid(ctx); // added by Boern
			SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, uid);
			String xml = csrb.buildXML(csc, data);
			HttpCommunication hc = new HttpCommunication();
			String ack = hc.postXML(Const.url, xml);
			SyncResponseHandler csrh = new SyncResponseHandler();
			return csrh.handleSearchACK(ack);
		}

	}

	private static class FirstSyncContactsCallable implements Callable {
		private Context ctx;
		private int page;
		private String uid;

		public FirstSyncContactsCallable(Context mContext, int page, String uid) {
			ctx = mContext;
			this.page = page;
			this.uid = uid;
		}

		public List<SyncRecord> call() throws Exception {
			SyncCommand csc = null;
			HashMap<String, String> data = new HashMap<String, String>();
			SyncRequestBuilder srb = new SyncRequestBuilder(ctx, uid);
			String xml = null;
			HttpCommunication hc = new HttpCommunication();
			SyncResponseHandler srh = new SyncResponseHandler();
			String ack = null;
			csc = SyncCommand.FIRSTSYNC;
			data.clear();
			// XXX
			data.put(XMLTag.FILTER.name(), "page=" + page + "&count=5"); // count=n&page=m&tag=x&keywords
			xml = srb.buildXML(csc, data);
//			Log.v(TAG + "Sync to First Sync requst :", xml);

			ack = hc.postXML(Const.url, xml);
//			Log.v(TAG + "Sync to First Sync response :", ack);

			return srh.handleSearchACK(ack);
		}
	}

	public static List<SyncRecord> firstSyncContacts(Context ctx, int page,
			String uid) throws InterruptedException, ExecutionException {
		FirstSyncContactsCallable f = new FirstSyncContactsCallable(ctx, page,
				uid);
		Future result = SyncActionPool.submit(f);
		return (List<SyncRecord>) result.get();
	}

	private static class RecoverContactsCallable implements Callable {
		private Context ctx;
		private int page;
		private String uid;

		public RecoverContactsCallable(Context context, int page) {
			ctx = context;
			this.page = page;

		}

		public List<SyncRecord> call() throws Exception {
			SyncCommand csc = SyncCommand.SEARCH;
			HashMap<String, String> data = new HashMap<String, String>();
			data.put(XMLTag.FILTER.name(), "tag=DELETED&page=" + page
					+ "&count=5");
			// SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, null);
			this.uid = Preferences.getUid(ctx); // added by Boern
			SyncRequestBuilder csrb = new SyncRequestBuilder(ctx, uid);
			String xml = csrb.buildXML(csc, data);
			HttpCommunication hc = new HttpCommunication();
			String ack = hc.postXML(Const.url, xml);
			SyncResponseHandler csrh = new SyncResponseHandler();
			return csrh.handleSearchACK(ack);
		}

	}

	public static List<SyncRecord> recoverContacts(Context ctx, int page)
			throws InterruptedException, ExecutionException {
		RecoverContactsCallable r = new RecoverContactsCallable(ctx, page);
		Future result = SyncActionPool.submit(r);
		return (List<SyncRecord>) result.get();
	}

	public static List<SyncRecord> retriveAchivedContacts(Context ctx,
			int page, int count, String queryString)
			throws InterruptedException, ExecutionException {
		RetriveAchivedContactsCallable r = new RetriveAchivedContactsCallable(
				ctx, page, count, queryString);
		Future result = SyncActionPool.submit(r);
		return (List<SyncRecord>) result.get();
	}

	private static class GetContactCountCallable implements Callable {

		private Context ctx;

		private String uid;

		public GetContactCountCallable(Context mContext, String uid) {
			ctx = mContext;
			this.uid = uid;
		}

		public Integer call() throws Exception {
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
			data.put(XMLTag.USERID.name(), uid); // 
			data.put(XMLTag.FILTER.name(), "tag=CACHED|DELETED"); // 
			xml = srb.buildXML(csc, data);
			ack = hc.postXML(Const.url, xml);
			return srh.handleGetCountACK(ack);
		}
	}

	/**
	 * Get all the sync contact count.
	 * 
	 * @author zhangbing@inspurworld.com
	 * @param user
	 * @return contact total count to sync
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public static int getSyncContactCount(Context ctx, String user)
			throws InterruptedException, ExecutionException {
		GetContactCountCallable getCountCall = new GetContactCountCallable(ctx,
				user);
		Future result = SyncActionPool.submit(getCountCall);
		return (Integer) result.get();

	}

}
