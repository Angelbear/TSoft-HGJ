package cn.edu.tsinghua.hpc.tmms.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContacts.Data;
import android.view.View;

public class TContactsContract {
	public static final String AUTHORITY = "cn.edu.tsinghua.hpc.tcontacts";
//	public static final String AUTHORITY = "com.android.contacts";
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
	public static final String REQUESTING_PACKAGE_PARAM_KEY = "requesting_package";

	public final static class RawContactsEntity {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "raw_contact_entities");
	}

	public final static class TData {
		public static final String RES_PACKAGE = "res_package";
		public static final String FOR_EXPORT_ONLY = "for_export_only";
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "data");
		public static Uri getContentUri(){
			if(TIntent.isTContactsInstalled)	return	TData.CONTENT_URI;
			else	return	android.provider.ContactsContract.Data.CONTENT_URI;
		}
	}

	public final static class TRawContacts {
		public static final String IS_RESTRICTED = "is_restricted";
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "raw_contacts");
		 public static Uri getContactLookupUri(ContentResolver resolver, Uri rawContactUri) {
	            // TODO: use a lighter query by joining rawcontacts with contacts in provider
	            final Uri dataUri = Uri.withAppendedPath(rawContactUri, Data.CONTENT_DIRECTORY);
	            final Cursor cursor = resolver.query(dataUri, new String[] {
	                    RawContacts.CONTACT_ID, Contacts.LOOKUP_KEY
	            }, null, null, null);

	            Uri lookupUri = null;
	            try {
	                if (cursor != null && cursor.moveToFirst()) {
	                    final long contactId = cursor.getLong(0);
	                    final String lookupKey = cursor.getString(1);
	                    return TContacts.getLookupUri(contactId, lookupKey);
	                }
	            } finally {
	                if (cursor != null) cursor.close();
	            }
	            return lookupUri;
	        }
	}

	public final static class TContacts {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "contacts");
		public static final Uri CONTENT_LOOKUP_URI = Uri.withAppendedPath(
				CONTENT_URI, "lookup");
		public static final Uri CONTENT_FILTER_URI = Uri.withAppendedPath(
				CONTENT_URI, "filter");
		public static final Uri CONTENT_STREQUENT_URI = Uri.withAppendedPath(
				CONTENT_URI, "strequent");
		public static final Uri CONTENT_STREQUENT_FILTER_URI = Uri
				.withAppendedPath(CONTENT_STREQUENT_URI, "filter");
		public static final Uri CONTENT_GROUP_URI = Uri.withAppendedPath(
				CONTENT_URI, "group");
		public static final Uri CONTENT_VCARD_URI = Uri.withAppendedPath(
				CONTENT_URI, "as_vcard");
		
		public static Uri getContentUri() {
			if(TIntent.isTContactsInstalled)	return TContacts.CONTENT_URI;
			else	return	ContactsContract.Contacts.CONTENT_URI;
		}
		
       public static Uri getLookupUri(ContentResolver resolver, Uri contactUri) {
           final Cursor c = resolver.query(contactUri, new String[] {
                   Contacts.LOOKUP_KEY, Contacts._ID
           }, null, null, null);
           if (c == null) {
               return null;
           }

           try {
               if (c.moveToFirst()) {
                   final String lookupKey = c.getString(0);
                   final long contactId = c.getLong(1);
                   return getLookupUri(contactId, lookupKey);
               }
           } finally {
               c.close();
           }
           return null;
       }

       public static Uri getLookupUri(long contactId, String lookupKey) {
    	   Uri tempUri = TContacts.CONTENT_LOOKUP_URI;
    	   if(!TIntent.isTContactsInstalled)	tempUri = Contacts.CONTENT_LOOKUP_URI;
           return ContentUris.withAppendedId(Uri.withAppendedPath(tempUri,
                   lookupKey), contactId);
       }
       
       public static Uri lookupContact(ContentResolver resolver, Uri lookupUri) {
           if (lookupUri == null) {
               return null;
           }

           Cursor c = resolver.query(lookupUri, new String[]{Contacts._ID}, null, null, null);
           if (c == null) {
               return null;
           }

           try {
               if (c.moveToFirst()) {
                   long contactId = c.getLong(0);
                   return ContentUris.withAppendedId(TContacts.getContentUri(), contactId);
               }
           } finally {
               c.close();
           }
           return null;
       }
	}

	public final static class TGroups {
		public static final String TITLE_RES = "title_res";
		public static final String RES_PACKAGE = "res_package";
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "groups");
		public static final Uri CONTENT_SUMMARY_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "groups_summary");
	}

	public final static class TPresence {

		public static final int OFFLINE = 0;
		public static final int DO_NOT_DISTURB = 1;
		public static final int AWAY = 2;
		public static final int IDLE = 3;
		public static final int AVAILABLE = 4;

		public static final int CLIENT_TYPE_DEFAULT = 0;
		public static final int CLIENT_TYPE_MOBILE = 1;

		public static final String IM_PROTOCOL = "im_protocol";
		public static final String PRESENCE_STATUS = TStatusUpdates.PRESENCE_STATUS;
		public static final String PRESENCE_CUSTOM_STATUS = TStatusUpdates.PRESENCE_CUSTOM_STATUS;
		public static final Uri CONTENT_URI = TStatusUpdates.CONTENT_URI;
	}

	public final static class TStatusUpdates {
		public static final String PRESENCE_STATUS = "mode";
		public static final String PRESENCE_CUSTOM_STATUS = "status";
		public static final int OFFLINE = 0x0;
		public static final int INVISIBLE = 0x1;
		public static final int AWAY = 0x2;
		public static final int IDLE = 0x3;
		public static final int DO_NOT_DISTURB = 0x4;
		public static final int AVAILABLE = 0x5;

		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "status_updates");

	}

	public final static class TPeople {
		public static final String PRESENCE_STATUS = "mode";
		public static final String PRESENCE_CUSTOM_STATUS = "status";
	}

	public static final class TSettings {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "settings");
	}

	public static final class TAggregationExceptions {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "aggregation_exceptions");
	}

	public static final class TPhone {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				TData.CONTENT_URI, "phones");
		public static final Uri CONTENT_FILTER_URI = Uri.withAppendedPath(
				CONTENT_URI, "filter");
		public static Uri getFilterUri() {
			if(TIntent.isTContactsInstalled)	return TPhone.CONTENT_FILTER_URI;
			else	return	ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI;
		}
	}

	public static final class TPhoneLookup {
		public static final Uri CONTENT_FILTER_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "phone_lookup");

		public static final Uri getFilterUri() {
			if(TIntent.isTContactsInstalled)	return TPhoneLookup.CONTENT_FILTER_URI;
			else	return	ContactsContract.PhoneLookup.CONTENT_FILTER_URI;
		}
	}

	public static final class TEmail {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				TData.CONTENT_URI, "emails");
		public static final Uri CONTENT_LOOKUP_URI = Uri.withAppendedPath(
				CONTENT_URI, "lookup");
		public static final Uri CONTENT_FILTER_URI = Uri.withAppendedPath(
				CONTENT_URI, "filter");
		public static Uri getLookupUri(){
			if(TIntent.isTContactsInstalled)	return TEmail.CONTENT_LOOKUP_URI;
			else	return	ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI;
		}
	}

	public static final class TStructuredPostal {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				TData.CONTENT_URI, "postals");
	}

	public static final class TQuickContact {
		public static final String ACTION_QUICK_CONTACT = TIntent.ACTION_QUICK_CONTACT;
		public static final String EXTRA_TARGET_RECT = "target_rect";
		public static final String EXTRA_MODE = "mode";
		public static final String EXTRA_EXCLUDE_MIMES = "exclude_mimes";
		public static void showQuickContact(Context context, View target, Uri lookupUri, int mode,
                String[] excludeMimes) {
            // Find location and bounds of target view
            final int[] location = new int[2];
            target.getLocationOnScreen(location);

            final Rect rect = new Rect();
            rect.left = location[0];
            rect.top = location[1];
            rect.right = rect.left + target.getWidth();
            rect.bottom = rect.top + target.getHeight();

            // Trigger with obtained rectangle
            showQuickContact(context, rect, lookupUri, mode, excludeMimes);
        }

        public static void showQuickContact(Context context, Rect target, Uri lookupUri, int mode,
                String[] excludeMimes) {
            // Launch pivot dialog through intent for now
            final Intent intent = new Intent(TIntent.getACTION_QUICK_CONTACT(context));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            intent.setData(lookupUri);
            intent.putExtra(EXTRA_TARGET_RECT, target);
            intent.putExtra(EXTRA_MODE, mode);
            intent.putExtra(EXTRA_EXCLUDE_MIMES, excludeMimes);
            context.startActivity(intent);
        }
	}

	public final static class TContactMethodsColumns {
		public static final String MOBILE_EMAIL_TYPE_NAME = "_AUTO_CELL";
	}
	
	public final static class TContentProviderOperation {
		public final static int TYPE_INSERT = 1;
	}
	
	public final static class Tstyleable {
		public static final int[] ContactsDataKind = {
            0x01010002, 0x01010026, 0x010102a2, 0x010102a3,
            0x010102a4
        };
	}
	
    public static final class TIntents {
        public static final String SHOW_OR_CREATE_CONTACT =
                "cn.edu.tsinghua.hpc.tcontacts.action.SHOW_OR_CREATE_CONTACT";
        
        public static String getSHOW_OR_CREATE_CONTACT(Context ctx){
    		if (TIntent.isTContactsInstalled(ctx))
    			return TIntents.SHOW_OR_CREATE_CONTACT;
    		else
    			return Intents.SHOW_OR_CREATE_CONTACT;
    	}
        
    }
}
