package cn.edu.tsinghua.hpc.tcontacts.provider;

import cn.edu.tsinghua.hpc.tcontacts.util.TMethod;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class TSyncProvider extends ContentProvider {
	private final String TABLE_TSYNC = "transparent_sync";
	private static final Uri NOTIFICATION_URI = Uri.parse("content://tcontact");
	private static final UriMatcher sURLMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);

	private static final int TCONTACT_USER_ID = 0;
	private static final int TCONTACT_LOCKED = 1;
	static {
		sURLMatcher.addURI("tcontact", "userid", TCONTACT_USER_ID);
		sURLMatcher.addURI("tcontact", "locked", TCONTACT_LOCKED);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new IllegalArgumentException("Invalid insert for uri "
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
		mOpenHelper = ContactsDatabaseHelper.getInstance(getContext());
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		// Init add the two values;
		ContentValues values = new ContentValues();
		values.put("key", "locked");
		values.put("value", "false");
		db.insert(TABLE_TSYNC, null, values);

		values.clear();
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
		case TCONTACT_LOCKED:
			qb.appendWhere("key = 'locked'");
			break;
		case TCONTACT_USER_ID:
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
		case TCONTACT_LOCKED:
			extraWhere = "key = 'locked'";
			break;
		case TCONTACT_USER_ID:
			extraWhere = "key = 'userid'";
			break;
		default:
			return 0;
		}
		selection = TMethod.concatenateWhere(selection, extraWhere);
		count = db.update(TABLE_TSYNC, values, selection, selectionArgs);

		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
			getContext().getContentResolver().notifyChange(NOTIFICATION_URI,
					null);
		}
		return count;
	}
}
