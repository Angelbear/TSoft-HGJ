package cn.edu.tsinghua.hpc.tmms.util;

import java.util.HashSet;
import java.util.Set;

import cn.edu.tsinghua.hpc.google.tmms.util.SqliteWrapper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract.StatusUpdates;
import android.provider.Telephony.BaseMmsColumns;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.TextBasedSmsColumns;
import android.provider.Telephony.ThreadsColumns;
import android.util.Log;

public class TTelephony {
	
	public static final class TMms{
		public static final String AUTHORITY = "tmms";
		public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

        public static final Uri CONTENT_URI = AUTHORITY_URI;
        public static final Uri REPORT_REQUEST_URI = Uri.withAppendedPath(
                                            CONTENT_URI, "report-request");
        public static final Uri REPORT_STATUS_URI = Uri.withAppendedPath(
                                            CONTENT_URI, "report-status");
        public static final class TDraft{
        	 public static final Uri
             CONTENT_URI = Uri.withAppendedPath(
            		 AUTHORITY_URI, "drafts");
        }
        public static final class TOutbox implements BaseColumns, TextBasedSmsColumns {
            public static final Uri CONTENT_URI = Uri.withAppendedPath(
           		 AUTHORITY_URI, "outbox");
		}

		public static final class TInbox {
			public static final Uri CONTENT_URI = Uri.withAppendedPath(
					AUTHORITY_URI, "inbox");
		}

		public static final class TSent {
			public static final Uri CONTENT_URI = Uri.withAppendedPath(
					AUTHORITY_URI, "sent");
		}

		public static final class TScrapSpace {
			public static final Uri CONTENT_URI = Uri.withAppendedPath(
					AUTHORITY_URI, "scrapSpace");
		}
		public static final class TRate {
            public static final Uri CONTENT_URI = Uri.withAppendedPath(
            		AUTHORITY_URI, "rate");
		}

	}

    public static final class TMmsSms{
    	public static final String AUTHORITY = "tmms-sms";
    	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

        public static final Uri CONTENT_URI = AUTHORITY_URI;
        public static final Uri CONTENT_CONVERSATIONS_URI = Uri.withAppendedPath(
                CONTENT_URI, "conversations");
        public static final Uri CONTENT_FILTER_BYPHONE_URI = Uri.withAppendedPath(
                CONTENT_URI, "messages/byphone");
        public static final Uri CONTENT_UNDELIVERED_URI = Uri.withAppendedPath(
                CONTENT_URI, "undelivered");
        public static final Uri CONTENT_DRAFT_URI = Uri.withAppendedPath(
                CONTENT_URI, "draft");
        public static final Uri CONTENT_LOCKED_URI = Uri.withAppendedPath(
                CONTENT_URI, "locked");
        public static final Uri SEARCH_URI = Uri.withAppendedPath(
                CONTENT_URI, "search");
        public static final class TPendingMessages implements BaseColumns {
            public static final Uri CONTENT_URI = Uri.withAppendedPath(
                    TMmsSms.CONTENT_URI, "pending");
        }
    }
    
	public static final class TSms implements BaseColumns, TextBasedSmsColumns {
		public static final Cursor query(ContentResolver cr, String[] projection) {
            return cr.query(CONTENT_URI, projection, null, null, DEFAULT_SORT_ORDER);
        }

        public static final Cursor query(ContentResolver cr, String[] projection,
                String where, String orderBy) {
            return cr.query(CONTENT_URI, projection, where,
                                         null, orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
        }
		public static final String AUTHORITY = "tsms";
		public static final Uri AUTHORITY_URI = Uri.parse("content://"
				+ AUTHORITY);
		public static final Uri CONTENT_URI = AUTHORITY_URI;
		public static final String DEFAULT_SORT_ORDER = "date DESC";
		public static Uri addMessageToUri(ContentResolver resolver,
                Uri uri, String address, String body, String subject,
                Long date, boolean read, boolean deliveryReport) {
            return addMessageToUri(resolver, uri, address, body, subject,
                    date, read, deliveryReport, -1L);
        }
		public static Uri addMessageToUri(ContentResolver resolver,
                Uri uri, String address, String body, String subject,
                Long date, boolean read, boolean deliveryReport, long threadId) {
            ContentValues values = new ContentValues(7);

            values.put(ADDRESS, address);
            if (date != null) {
                values.put(DATE, date);
            }
            values.put(READ, read ? Integer.valueOf(1) : Integer.valueOf(0));
            values.put(SUBJECT, subject);
            values.put(BODY, body);
            if (deliveryReport) {
                values.put(STATUS, STATUS_PENDING);
            }
            if (threadId != -1L) {
                values.put(THREAD_ID, threadId);
            }
            return resolver.insert(uri, values);
        }
		public static final class TConversations {
			public static final Uri CONTENT_URI = Uri.withAppendedPath(
					AUTHORITY_URI, "conversations");
		}
		public static final class TOutbox {
            public static final Uri CONTENT_URI = Uri.withAppendedPath(
           		 AUTHORITY_URI, "outbox");
            public static Uri addMessage(ContentResolver resolver,
                    String address, String body, String subject, Long date,
                    boolean deliveryReport, long threadId) {
                return addMessageToUri(resolver, CONTENT_URI, address, body,
                        subject, date, true, deliveryReport, threadId);
            }
        }
        public static final class TInbox {
            public static final Uri CONTENT_URI = Uri.withAppendedPath(
           		 AUTHORITY_URI, "inbox");
            public static Uri addMessage(ContentResolver resolver,
                    String address, String body, String subject, Long date,
                    boolean read) {
                return addMessageToUri(resolver, CONTENT_URI, address, body,
                        subject, date, read, false);
            }
        }
        public static final class TSent implements BaseColumns, TextBasedSmsColumns {
            public static final Uri CONTENT_URI = Uri.withAppendedPath(
           		 AUTHORITY_URI, "sent");
            public static Uri addMessage(ContentResolver resolver,
                    String address, String body, String subject, Long date) {
                return addMessageToUri(resolver, CONTENT_URI, address, body,
                        subject, date, true, false);
            }
        }
        public static final class TDraft implements BaseColumns, TextBasedSmsColumns {
        	public static final Uri CONTENT_URI = Uri.withAppendedPath(
              		 AUTHORITY_URI, "draft");
        	public static Uri addMessage(ContentResolver resolver,
                    String address, String body, String subject, Long date) {
                return addMessageToUri(resolver, CONTENT_URI, address, body,
                        subject, date, true, false);
            }
        	public static boolean saveMessage(ContentResolver resolver,
                    Uri uri, String body) {
                ContentValues values = new ContentValues(2);
                values.put(BODY, body);
                values.put(DATE, System.currentTimeMillis());
                return resolver.update(uri, values, null, null) == 1;
            }
        }
	}
	
	public static final class TMessage {
		public static final String AUTHORITY = "tmessage";
		public static final Uri AUTHORITY_URI = Uri.parse("content://"
				+ AUTHORITY);
	}
    
    public static final class TCarriers implements BaseColumns {
        public static final Uri CONTENT_URI =
            Uri.parse("content://ttelephony/carriers");
    }
	
    public class TConnectivityManager{
    	public static final int TYPE_MOBILE_MMS  = 2;
    }
    
    public static final class TPresence{
    	public static final int OFFLINE = 0;
    }
    
    public final class TMediaStore {
    	public final static String EXTRA_SIZE_LIMIT = "android.intent.extra.sizeLimit";
    	public final static String EXTRA_DURATION_LIMIT = "android.intent.extra.durationLimit";
    }
/*    
	public static final class TThreads {
		private static final Uri THREAD_ID_CONTENT_URI = Uri
				.parse("content://tmms-sms/threadID");
		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				TMmsSms.CONTENT_URI, "conversations");
		public static final Uri OBSOLETE_THREADS_URI = Uri.withAppendedPath(
				CONTENT_URI, "obsolete");
	}
*/	
    public static final class TThreads implements ThreadsColumns {
        private static final String[] ID_PROJECTION = { BaseColumns._ID };
        private static final String STANDARD_ENCODING = "UTF-8";
        private static final Uri THREAD_ID_CONTENT_URI = Uri.parse(
                "content://tmms-sms/threadID");
        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                TMmsSms.CONTENT_URI, "conversations");
        public static final Uri OBSOLETE_THREADS_URI = Uri.withAppendedPath(
                CONTENT_URI, "obsolete");

        public static final int COMMON_THREAD    = 0;
        public static final int BROADCAST_THREAD = 1;

        // No one should construct an instance of this class.
        private TThreads() {
        }

        public static long getOrCreateThreadId(Context context, String recipient) {
            Set<String> recipients = new HashSet<String>();

            recipients.add(recipient);
            return getOrCreateThreadId(context, recipients);
        }

        public static long getOrCreateThreadId(
                Context context, Set<String> recipients) {
            Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();

            for (String recipient : recipients) {
                if (Mms.isEmailAddress(recipient)) {
                    recipient = Mms.extractAddrSpec(recipient);
                }

                uriBuilder.appendQueryParameter("recipient", recipient);
            }

            Uri uri = uriBuilder.build();
            String TAG = "TThreads";
			if (true) {
                Log.v(TAG, "getOrCreateThreadId uri: " + uri);
            }
            Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                    uri, ID_PROJECTION, null, null, null);
            if (true) {
                Log.v(TAG, "getOrCreateThreadId cursor cnt: " + cursor.getCount());
            }
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        return cursor.getLong(0);
                    } else {
                        Log.e(TAG, "getOrCreateThreadId returned no rows!");
                    }
                } finally {
                    cursor.close();
                }
            }

            Log.e(TAG, "getOrCreateThreadId failed with uri " + uri.toString());
            throw new IllegalArgumentException("Unable to find or allocate a thread ID.");
        }
    }


}
