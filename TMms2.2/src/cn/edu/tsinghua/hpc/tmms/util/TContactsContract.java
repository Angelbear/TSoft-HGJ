package cn.edu.tsinghua.hpc.tmms.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.DisplayMetrics;
import android.view.View;


public class TContactsContract {
	public static final String AUTHORITY = "cn.edu.tsinghua.hpc.tcontacts";
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
	public static final String REQUESTING_PACKAGE_PARAM_KEY = "requesting_package";

	public final static class TRawContactsEntity {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "raw_contact_entities");
	}

	public final static class TData {
		public static final String RES_PACKAGE = "res_package";
		public static final String FOR_EXPORT_ONLY = "for_export_only";
        public static final String CONTENT_DIRECTORY = "data";
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "data");
	}

	public interface TDisplayNameSources {
		public static final int UNDEFINED = 0;
		public static final int EMAIL = 10;
		public static final int PHONE = 20;
		public static final int ORGANIZATION = 30;
		public static final int NICKNAME = 35;
		public static final int STRUCTURED_NAME = 40;
	}

	public final static class TContactMethodsColumns {
		public static final String MOBILE_EMAIL_TYPE_NAME = "_AUTO_CELL";
	}
	
	public interface TFullNameStyle {
		public static final int UNDEFINED = 0;
		public static final int WESTERN = 1;
		public static final int CJK = 2;
        public static final int CHINESE = 3;
        public static final int JAPANESE = 4;
        public static final int KOREAN = 5;
	}

	protected interface TContactNameColumns {
		public static final String DISPLAY_NAME_SOURCE = "display_name_source";
		public static final String DISPLAY_NAME_PRIMARY = "display_name";
		public static final String DISPLAY_NAME_ALTERNATIVE = "display_name_alt";
		public static final String PHONETIC_NAME = "phonetic_name";
		public static final String PHONETIC_NAME_STYLE = "phonetic_name_style";
		public static final String SORT_KEY_PRIMARY = "sort_key";
		public static final String SORT_KEY_ALTERNATIVE = "sort_key_alt";
	}
	
	public final static class TContactCounts {
        public static final String ADDRESS_BOOK_INDEX_EXTRAS = "address_book_index_extras";
        public static final String EXTRA_ADDRESS_BOOK_INDEX_TITLES = "address_book_index_titles";
        public static final String EXTRA_ADDRESS_BOOK_INDEX_COUNTS = "address_book_index_counts";
    }

	public final static class TRawContacts implements TContactNameColumns {
		public static final String IS_RESTRICTED = "is_restricted";
		public static final String NAME_VERIFIED = "name_verified";

		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "raw_contacts");
		public static Uri getContactLookupUri(ContentResolver resolver, Uri rawContactUri) {
            final Uri dataUri = Uri.withAppendedPath(rawContactUri, TData.CONTENT_DIRECTORY);
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

	public static final class CommonDataKinds {
		public static final String DATA1 = "data1";
		public static final String DATA10 = "data10";
		public static final String DATA11 = "data11";

		public static final class TOrganization {
			public static final String PHONETIC_NAME_STYLE = DATA10;
		}

		public static final class TStructuredName {
			public static final String FULL_NAME_STYLE = DATA10;
			public static final String PHONETIC_NAME_STYLE = DATA11;
		}

		public static final class TEmail {
			public static final String ADDRESS = DATA1;
			public static final Uri CONTENT_URI = Uri.withAppendedPath(
					TData.CONTENT_URI, "emails");
			public static final Uri CONTENT_LOOKUP_URI = Uri.withAppendedPath(
					CONTENT_URI, "lookup");
			public static final Uri CONTENT_FILTER_URI = Uri.withAppendedPath(
					CONTENT_URI, "filter");
		}
	}

	public final static class TContacts implements TContactNameColumns {
		public static final String NAME_RAW_CONTACT_ID = "name_raw_contact_id";
		public static final String AUTHORITY = "tcontacts";
		public static final class TPhoto implements DataColumns {
            public static final String CONTENT_DIRECTORY = "photo";
            public static final String PHOTO = DATA15;
        }
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
		public static final Uri CONTENT_MULTI_VCARD_URI = Uri.withAppendedPath(CONTENT_URI,
        "as_multi_vcard");
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
                    return ContentUris.withAppendedId(TContacts.CONTENT_URI, contactId);
                }
            } finally {
                c.close();
            }
            return null;
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
            return ContentUris.withAppendedId(Uri.withAppendedPath(TContacts.CONTENT_LOOKUP_URI,
                    lookupKey), contactId);
        }
	}

	public static class TSearchSnippetColumns {
		public static final String SNIPPET_DATA_ID = "snippet_data_id";
		public static final String SNIPPET_MIMETYPE = "snippet_mimetype";
		public static final String SNIPPET_DATA1 = "snippet_data1";
		public static final String SNIPPET_DATA2 = "snippet_data2";
		public static final String SNIPPET_DATA3 = "snippet_data3";
		public static final String SNIPPET_DATA4 = "snippet_data4";

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
	
	public static final class TQuickContact {
		public static final String ACTION_QUICK_CONTACT = TIntent.ACTION_QUICK_CONTACT;
        public static final String EXTRA_TARGET_RECT = "target_rect";
        public static final String EXTRA_MODE = "mode";
        public static final String EXTRA_EXCLUDE_MIMES = "exclude_mimes";
        private static CompatibilityInfo mCompatibilityInfo;
        public static void showQuickContact(Context context, View target, Uri lookupUri, int mode,
                String[] excludeMimes) {
        	//mCompatibilityInfo = context.getResources().getCompatibilityInfo();
        	mCompatibilityInfo = TMethod.getCompatibilityInfo(context.getResources());
        	final float appScale = mCompatibilityInfo.applicationScale;
            final int[] pos = new int[2];
            target.getLocationOnScreen(pos);

            final Rect rect = new Rect();
            rect.left = (int) (pos[0] * appScale + 0.5f);
            rect.top = (int) (pos[1] * appScale + 0.5f);
            rect.right = (int) ((pos[0] + target.getWidth()) * appScale + 0.5f);
            rect.bottom = (int) ((pos[1] + target.getHeight()) * appScale + 0.5f);
            showQuickContact(context, rect, lookupUri, mode, excludeMimes);
        }
        public static void showQuickContact(Context context, Rect target, Uri lookupUri, int mode,
                String[] excludeMimes) {
            final Intent intent = new Intent(ACTION_QUICK_CONTACT);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            intent.setData(lookupUri);
            intent.setSourceBounds(target);
            intent.putExtra(EXTRA_MODE, mode);
            intent.putExtra(EXTRA_EXCLUDE_MIMES, excludeMimes);
            context.startActivity(intent);
        }
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

	public static final class TProviderStatus {
		public static final String STATUS = "status";
		public static final int STATUS_NORMAL = 0;
		public static final int STATUS_UPGRADING = 1;
		public static final int STATUS_UPGRADE_OUT_OF_MEMORY = 2;
		public static final int STATUS_CHANGING_LOCALE = 3;
		public static final String DATA1 = "data1";
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "provider_status");
	}

	public static final class TPreferences {
        public static final String SORT_ORDER = "android.contacts.SORT_ORDER";
        public static final int SORT_ORDER_PRIMARY = 1;
        public static final int SORT_ORDER_ALTERNATIVE = 2;
        public static final String DISPLAY_ORDER = "android.contacts.DISPLAY_ORDER";
        public static final int DISPLAY_ORDER_PRIMARY = 1;
        public static final int DISPLAY_ORDER_ALTERNATIVE = 2;
    }
	
	public interface TPhoneticNameStyle {
		public static final int UNDEFINED = 0;
		public static final int PINYIN = 3;
		public static final int JAPANESE = 4;
		public static final int KOREAN = 5;
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
	}

	public static final class TPhoneLookup {
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/phone_lookup";
		public static final Uri CONTENT_FILTER_URI = Uri.withAppendedPath(
				AUTHORITY_URI, "phone_lookup");
	}

	public static final class TStructuredPostal {
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				TData.CONTENT_URI, "postals");
	}
	
	protected interface DataColumns {
        public static final String RES_PACKAGE = "res_package";
        public static final String MIMETYPE = "mimetype";
        public static final String RAW_CONTACT_ID = "raw_contact_id";
        public static final String IS_PRIMARY = "is_primary";
        public static final String IS_SUPER_PRIMARY = "is_super_primary";
        public static final String DATA_VERSION = "data_version";
        public static final String DATA1 = "data1";
        public static final String DATA2 = "data2";
        public static final String DATA3 = "data3";
        public static final String DATA4 = "data4";
        public static final String DATA5 = "data5";
        public static final String DATA6 = "data6";
        public static final String DATA7 = "data7";
        public static final String DATA8 = "data8";
        public static final String DATA9 = "data9";
        public static final String DATA10 = "data10";
        public static final String DATA11 = "data11";
        public static final String DATA12 = "data12";
        public static final String DATA13 = "data13";
        public static final String DATA14 = "data14";
        public static final String DATA15 = "data15";
        public static final String SYNC1 = "data_sync1";
        public static final String SYNC2 = "data_sync2";
        public static final String SYNC3 = "data_sync3";
        public static final String SYNC4 = "data_sync4";
    }

	public static final class Intents {
		public static final class UI {
			public static final String LIST_DEFAULT = "com.android.contacts.action.LIST_DEFAULT";
			public static final String LIST_GROUP_ACTION = "com.android.contacts.action.LIST_GROUP";
			public static final String GROUP_NAME_EXTRA_KEY = "com.android.contacts.extra.GROUP";
			public static final String LIST_ALL_CONTACTS_ACTION = "com.android.contacts.action.LIST_ALL_CONTACTS";
			public static final String LIST_CONTACTS_WITH_PHONES_ACTION = "com.android.contacts.action.LIST_CONTACTS_WITH_PHONES";
			public static final String LIST_STARRED_ACTION = "com.android.contacts.action.LIST_STARRED";
			public static final String LIST_FREQUENT_ACTION = "com.android.contacts.action.LIST_FREQUENT";
			public static final String LIST_STREQUENT_ACTION = "com.android.contacts.action.LIST_STREQUENT";
			public static final String TITLE_EXTRA_KEY = "com.android.contacts.extra.TITLE_EXTRA";
			public static final String FILTER_CONTACTS_ACTION = "com.android.contacts.action.FILTER_CONTACTS";
			public static final String FILTER_TEXT_EXTRA_KEY = "com.android.contacts.extra.FILTER_TEXT";
		}
	}
}
