package cn.edu.tsinghua.hpc.tmms.syncaction;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TaskDBAdaptor {
	public static final String KEY_ROWID = "_id";

	public static final String KEY_CMD = "cmd";

	public static final String KEY_LOCAL_ID = "local_id";

	public static final String KEY_FILTER = "filter";

	private static final String TAG = "TaskDBAdaptor";

	private static final String DATABASE_NAME = "mycontacts";

	private static final String DATABASE_TABLE = "task";

	private static final int DATABASE_VERSION = 1;

	private static final String DATABASE_CREATE =

	"create table task (_id integer primary key autoincrement, "

	+ "cmd text not null, local_id text null, filter text null);";

	private final Context context;

	private DatabaseHelper DBHelper;

	private SQLiteDatabase db;

	public TaskDBAdaptor(Context ctx) {

		this.context = ctx;
		DBHelper = new DatabaseHelper(context);
	}

	public TaskDBAdaptor open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		DBHelper.close();
	}

	public long insertTask(String cmd, String localId) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_CMD, cmd);
		initialValues.put(KEY_LOCAL_ID, localId);
		return db.insert(DATABASE_TABLE, null, initialValues);

	}

	public long insertFilterTask(String cmd, String localId ,String filter) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_CMD, cmd);
		initialValues.put(KEY_LOCAL_ID, localId);
		initialValues.put(KEY_FILTER, filter);
		return db.insert(DATABASE_TABLE, null, initialValues);
	}

	public boolean deleteTask(String localId) {
		Log.d("MyContact", "deleteTask");
		return db.delete(DATABASE_TABLE, KEY_LOCAL_ID + "=" + localId, null) > 0;
	}

	public Cursor getAllTask() {
		return db.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_CMD,
				KEY_LOCAL_ID, KEY_FILTER }, null, null, null, null, null);
	}

	public Task getTaskByLocalId(String localId) {
		try {
			Cursor c = db.query(DATABASE_TABLE, new String[] { KEY_CMD,
					KEY_LOCAL_ID, KEY_FILTER }, KEY_LOCAL_ID + "=" + localId,
					null, null, null, null);
			if (c != null && c.moveToFirst()) {
				return new Task(c.getString(c.getColumnIndex(KEY_CMD)), c
						.getString(c.getColumnIndex(KEY_LOCAL_ID)));
			}
		} catch (Exception e) {

		}
		return null;

	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS task");
			onCreate(db);

		}
	}
}
