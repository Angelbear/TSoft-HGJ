package cn.edu.tsinghua.hpc.tcontacts.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class TCallLog {
	public static final String AUTHORITY = "tcall_log";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	public static final String NUMBER = "number";
	public static final String TYPE = "type";
    public static final int OUTGOING_TYPE = 2;
    public static final String DEFAULT_SORT_ORDER = "date DESC";

	public static final class TCalls {
		public static final Uri CONTENT_URI = Uri
				.parse("content://tcall_log/calls");

		public static String getLastOutgoingCall(Context context) {
			final ContentResolver resolver = context.getContentResolver();
			Cursor c = null;
			try {
				c = resolver.query(CONTENT_URI, new String[] { NUMBER }, TYPE
						+ " = " + OUTGOING_TYPE, null, DEFAULT_SORT_ORDER
						+ " LIMIT 1");
				if (c == null || !c.moveToFirst()) {
					return "";
				}
				return c.getString(0);
			} finally {
				if (c != null)
					c.close();
			}
		}
	}

}
