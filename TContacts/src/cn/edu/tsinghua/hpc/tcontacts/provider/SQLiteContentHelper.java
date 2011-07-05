package cn.edu.tsinghua.hpc.tcontacts.provider;

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Some helper functions for using SQLite database to implement content providers.
 *
 * @hide
 */
public class SQLiteContentHelper {

    /**
     * Runs an SQLite query and returns an AssetFileDescriptor for the
     * blob in column 0 of the first row. If the first column does
     * not contain a blob, an unspecified exception is thrown.
     *
     * @param db Handle to a readable database.
     * @param sql SQL query, possibly with query arguments.
     * @param selectionArgs Query argument values, or {@code null} for no argument.
     * @return If no exception is thrown, a non-null AssetFileDescriptor is returned.
     * @throws FileNotFoundException If the query returns no results or the
     *         value of column 0 is NULL, or if there is an error creating the
     *         asset file descriptor.
     */
    public static AssetFileDescriptor getBlobColumnAsAssetFile(SQLiteDatabase db, String sql,
            String[] selectionArgs) throws FileNotFoundException {
        try {
            MemoryFile file = simpleQueryForBlobMemoryFile(db, sql, selectionArgs);
            if (file == null) {
                throw new FileNotFoundException("No results.");
            }
            
            // XXX
            // Use hack
            Method methodFromMemoryFile = Class.forName("android.content.res.AssetFileDescriptor").getMethod("fromMemoryFile", new Class[] {MemoryFile.class});
			methodFromMemoryFile.setAccessible(true);
			AssetFileDescriptor fd = (AssetFileDescriptor) methodFromMemoryFile.invoke(null, new Object[] {file});
            return fd;
            
        } catch (IOException ex) {
            throw new FileNotFoundException(ex.toString());
        } catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (ClassNotFoundException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
		return null;
    }

    /**
     * Runs an SQLite query and returns a MemoryFile for the
     * blob in column 0 of the first row. If the first column does
     * not contain a blob, an unspecified exception is thrown.
     *
     * @return A memory file, or {@code null} if the query returns no results
     *         or the value column 0 is NULL.
     * @throws IOException If there is an error creating the memory file.
     */
    // TODO: make this native and use the SQLite blob API to reduce copying
    private static MemoryFile simpleQueryForBlobMemoryFile(SQLiteDatabase db, String sql,
            String[] selectionArgs) throws IOException {
        Cursor cursor = db.rawQuery(sql, selectionArgs);
        if (cursor == null) {
            return null;
        }
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            byte[] bytes = cursor.getBlob(0);
            if (bytes == null) {
                return null;
            }
            MemoryFile file = new MemoryFile(null, bytes.length);
            file.writeBytes(bytes, 0, 0, bytes.length);
            // XXX
            //file.deactivate();
            return file;
        } finally {
            cursor.close();
        }
    }

}
