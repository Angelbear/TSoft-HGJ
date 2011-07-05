package cn.edu.tsinghua.hpc.tcontacts.service;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

public class TContactSyncHelper {
	public static boolean tryLock(Context ctx) {
		Uri uri = Uri.parse("content://tcontact/locked");
		Cursor c = ctx.getContentResolver().query(uri,
				new String[] { "value" }, null, null, null);
		if (c != null && c.moveToFirst()) {
			if (Boolean.getBoolean(c.getString((c.getColumnIndex("value"))))) {
				// The db is locked
				c.close();
				return false;
			} else {
				c.close();
				ContentValues values = new ContentValues();
				values.put("value", "true");
				if (ctx.getContentResolver().update(uri, values, null, null) > 0)
					return true;
			}
		}
		return false;
	}

	private static Lock lock = new ReentrantLock();
	private static Condition threadCond = lock.newCondition();

	public static boolean lock(Context ctx) {
		// 99% goes here
		if (tryLock(ctx))
			return true;
		
		// Unfortunately goes here, then we will wait until the lock is released
		lock.lock();
		LockContentObserver observer = new LockContentObserver(new Handler());
		ctx.getContentResolver().registerContentObserver(
				Uri.parse("content://tcontact/locked"), true,
				new LockContentObserver(null) {
				});
		try {
			threadCond.await();
			if (tryLock(ctx)) {
				ctx.getContentResolver().unregisterContentObserver(observer);
				return true;
			}
		} catch (InterruptedException e) {
			
		} finally {
			ctx.getContentResolver().unregisterContentObserver(observer);
			lock.unlock();
		}
		
		return false;
	}

	private static class LockContentObserver extends ContentObserver {
		public LockContentObserver(Handler handler) {
			super(handler);
			lock.lock();
		}

		@Override
		public void onChange(boolean selfChange) {
			threadCond.signal();
			lock.unlock();
		}

		@Override
		public boolean deliverSelfNotifications() {
			return super.deliverSelfNotifications();
		}
	}

	public static boolean releaseLock(Context ctx) {
		Uri uri = Uri.parse("content://tcontact/locked");
		ContentValues values = new ContentValues();
		values.put("value", "false");
		if (ctx.getContentResolver().update(uri, values, null, null) > 0)
			return true;
		return false;
	}
}
