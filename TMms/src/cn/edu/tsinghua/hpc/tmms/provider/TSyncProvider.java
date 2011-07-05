package cn.edu.tsinghua.hpc.tmms.provider;

import java.lang.reflect.Method;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class TSyncProvider extends ContentProvider {
	private final String TABLE_TSYNC = "transparent_sync";
	private static final Uri NOTIFICATION_URI = Uri.parse("content://tmessage");//Uri.parse("content://tmessage");
	private static final UriMatcher sURLMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);

	private static final String VND_ANDROID_MESSAGE_LOCKED = "vnd.android.cursor.item/message_locked";
	private static final String VND_ANDROID_MESSAGE_USERID = "vnd.android.cursor.item/message_userid";

	private static final int TMESSAGE_USER_ID = 0;
	private static final int TMESSAGE_LOCKED = 1;
	private static final String TAG = "TSyncProvider";
	static {
		sURLMatcher.addURI("tmessage", "userid", TMESSAGE_USER_ID);
		sURLMatcher.addURI("tmessage", "locked", TMESSAGE_LOCKED);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new IllegalArgumentException("Invalid delete for uri "
				+ uri.toString());
	}

	@Override
	public String getType(Uri uri) {
		// XXX
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new IllegalArgumentException("Invalid insert for uri "
				+ uri.toString());
	}

	private SQLiteOpenHelper mOpenHelper;

	@Override
	public boolean onCreate() {
		mOpenHelper = MmsSmsDatabaseHelper.getInstance(getContext());
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		// Init add the two values;
		ContentValues values = new ContentValues();
		values.clear();
//		values.put("_id", 0);
		values.put("key", "locked");
		values.put("value", "false");
		db.insert(TABLE_TSYNC, null, values);

		values.clear();
//		values.put("_id", 1);
		values.put("key", "userid");
		db.insert(TABLE_TSYNC, null, values);

		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(TABLE_TSYNC);
		// Generate the body of the query.
		int match = sURLMatcher.match(uri);
		switch (match) {
		case TMESSAGE_LOCKED:
			qb.appendWhere("key = 'locked'");
			break;
		case TMESSAGE_USER_ID:
			qb.appendWhere("key = 'userid'");
			break;
		default:
			return null;
		}
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor ret = qb.query(db, projection, selection, selectionArgs, null,
				null, sortOrder);
		ret.setNotificationUri(getContext().getContentResolver(),
				NOTIFICATION_URI);
		return ret;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count = 0;
		int match = sURLMatcher.match(uri);
		String extraWhere = null;
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		switch (match) {
		case TMESSAGE_LOCKED:
			extraWhere = "key = 'locked'";
			break;
		case TMESSAGE_USER_ID:
			extraWhere = "key = 'userid'";
			break;
		default:
			return 0;
		}
		
		try {
			Method concatenateWhereMethod = Class.forName("android.database.DatabaseUtils")
					.getMethod("concatenateWhere",
							new Class[] { String.class, String.class });
			concatenateWhereMethod.setAccessible(true);
			selection = (String) concatenateWhereMethod.invoke(null, new Object[] {
					selection, extraWhere });
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}
		
//		selection = DatabaseUtils.concatenateWhere(selection, extraWhere);
		count = db.update(TABLE_TSYNC, values, selection, selectionArgs);

		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
			getContext().getContentResolver().notifyChange(NOTIFICATION_URI,
					null);
		}
		return count;
	}
}
