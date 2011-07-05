/*
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
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

package cn.edu.tsinghua.hpc.tmms.transaction;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Sms;
import android.telephony.SmsMessage;
import android.util.Log;

import cn.edu.tsinghua.hpc.google.tmms.util.SqliteWrapper;
import cn.edu.tsinghua.hpc.tmms.LogTag;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMmsSms;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TSms;


public class MessageStatusReceiver extends BroadcastReceiver {
    public static final String MESSAGE_STATUS_RECEIVED_ACTION =
            "cn.edu.tsinghua.hpc.tmms.transaction.MessageStatusReceiver.MESSAGE_STATUS_RECEIVED";
    private static final String[] ID_PROJECTION = new String[] { Sms._ID };
    private static final String LOG_TAG = "MessageStatusReceiver";
    private static final Uri STATUS_URI =
    	Uri.withAppendedPath(TSms.AUTHORITY_URI, "status");
           // Uri.parse("content://sms/status");


    @Override
    public void onReceive(Context context, Intent intent) {

        if (MESSAGE_STATUS_RECEIVED_ACTION.equals(intent.getAction())) {

            Uri messageUri = intent.getData();
            
            byte[] pdu = intent.getStringExtra("pdu").getBytes();
            //byte[] pdu = (byte[]) intent.getExtra("pdu");

            updateMessageStatus(context, messageUri, pdu);
            MessagingNotification.updateNewMessageIndicator(context, true);
       }
    }

    private void updateMessageStatus(Context context, Uri messageUri, byte[] pdu) {
        // Create a "status/#" URL and use it to update the
        // message's status in the database.
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                            messageUri, ID_PROJECTION, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                int messageId = cursor.getInt(0);

                Uri updateUri = ContentUris.withAppendedId(STATUS_URI, messageId);
                SmsMessage message = SmsMessage.createFromPdu(pdu);
                int status = message.getStatus();
                ContentValues contentValues = new ContentValues(1);

                if (Log.isLoggable(LogTag.TAG, Log.DEBUG)) {
                    log("updateMessageStatus: msgUrl=" + messageUri + ", status=" + status);
                }

                contentValues.put(Sms.STATUS, status);
                SqliteWrapper.update(context, context.getContentResolver(),
                                    updateUri, contentValues, null, null);
            } else {
                error("Can't find message for status update: " + messageUri);
            }
        } finally {
            cursor.close();
        }
    }

    private void error(String message) {
        Log.e(LOG_TAG, "[MessageStatusReceiver] " + message);
    }
    
    private void log(String message) {
        Log.d(LOG_TAG, "[MessageStatusReceiver] " + message);
    }
}
