package cn.edu.tsinghua.hpc.tcontacts.syncaction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.pim.vcard.exception.VCardException;
import cn.edu.tsinghua.hpc.syncbroker.ContactRecord;
import cn.edu.tsinghua.hpc.syncbroker.ElementNotFound;
import cn.edu.tsinghua.hpc.syncbroker.SyncRecord;
import cn.edu.tsinghua.hpc.tcontacts.pim.ContactStruct;
import cn.edu.tsinghua.hpc.tcontacts.pim.VCardConfig;
import cn.edu.tsinghua.hpc.tcontacts.pim.VCardDataBuilder;
import cn.edu.tsinghua.hpc.tcontacts.service.BootReceiver;
import cn.edu.tsinghua.hpc.vcard.VCardParser;
import cn.edu.tsinghua.hpc.vcard.VCardParser_V21;

public class ContactsUtility {

	public static void notifySyncService(Context ctx) {
		Intent i = new Intent(ctx, BootReceiver.class);
		i.setAction(BootReceiver.TSYNC_REQUEST);
		ctx.sendBroadcast(i);
	}

	public static int setGuid(Context ctx, Uri uri, int guid) {
		ContentValues values = new ContentValues();
		values.put("guid", guid);
		return ctx.getContentResolver().update(uri, values, null, null);
	}

	public static int markContact(Context ctx, Uri uri, String syncState) {
		ContentValues values = new ContentValues();
		values.put("sync_state", syncState);
		return ctx.getContentResolver().update(uri, values, null, null);
	}

	public static List<ContactStruct> getInfoHolderFromResult(
			List<SyncRecord> records) throws ClientProtocolException,
			ElementNotFound, IOException, VCardException {
		List<ContactStruct> mContactsInfoHolder = new ArrayList<ContactStruct>();
		// TODO:
		VCardDataBuilder builder;
		for (SyncRecord s : records) {
			ContactRecord r = (ContactRecord) s;
			builder = new VCardDataBuilder("UTF-8", "UTF-8", false,
					VCardConfig.VCARD_TYPE_V21_GENERIC, null);
			VCardParser p = new VCardParser_V21();
			p.parse(new ByteArrayInputStream(r.getData().getBytes()), "UTF-8",
					builder);
			ContactStruct contact = builder.mLastContactStruct;
			contact.guid = r.getGuid();
			mContactsInfoHolder.add(contact);
		}
		return mContactsInfoHolder;
	}
	

	
//	/**
//	 * @author zhangbing@inpurworld.com
//	 * @param ctx
//	 * @return LoginView
//	 * @throws Exception
//	 */
//	public static LoginView login(Context ctx,Handler mHandler) throws Exception{
//	    	
//	    	CCITSC mCCIT = new CCITSC(ctx, "221.122.98.38", "8808",mHandler);
////	    	CCITSC mCCIT = new CCITSC(ctx, "211.139.191.207", "8080");
//	    	mCCIT.loginInit(false);
//	    	LoginView login = mCCIT.requestLogin(false);
//	    	return login;
//	    	
//	    }
}
