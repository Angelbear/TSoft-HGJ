/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import cn.edu.tsinghua.hpc.google.tmms.InvalidHeaderValueException;
import cn.edu.tsinghua.hpc.google.tmms.MmsException;
import cn.edu.tsinghua.hpc.google.tmms.pdu.EncodedStringValue;
import cn.edu.tsinghua.hpc.google.tmms.pdu.GenericPdu;
import cn.edu.tsinghua.hpc.google.tmms.pdu.PduHeaders;
import cn.edu.tsinghua.hpc.google.tmms.pdu.PduPersister;
import cn.edu.tsinghua.hpc.google.tmms.pdu.ReadRecInd;
import cn.edu.tsinghua.hpc.google.tmms.pdu.SendReq;
import cn.edu.tsinghua.hpc.tmms.ui.MessagingPreferenceActivity;
import cn.edu.tsinghua.hpc.tmms.util.SendingProgressTokenManager;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TMms;


public class MmsMessageSender implements MessageSender {
    private static final String TAG = "MmsMessageSender";

    private final Context mContext;
    private final Uri mMessageUri;
    private final long mMessageSize;

    // Default preference values
    private static final boolean DEFAULT_DELIVERY_REPORT_MODE  = false;
    private static final boolean DEFAULT_READ_REPORT_MODE      = false;
    private static final long    DEFAULT_EXPIRY_TIME     = 7 * 24 * 60 * 60;
    private static final int     DEFAULT_PRIORITY        = PduHeaders.PRIORITY_NORMAL;
    private static final String  DEFAULT_MESSAGE_CLASS   = PduHeaders.MESSAGE_CLASS_PERSONAL_STR;

    public MmsMessageSender(Context context, Uri location, long messageSize) {
        mContext = context;
        mMessageUri = location;
        mMessageSize = messageSize;

        if (mMessageUri == null) {
            throw new IllegalArgumentException("Null message URI.");
        }
    }

    public boolean sendMessage(long token) throws MmsException {
        // Load the MMS from the message uri
        PduPersister p = PduPersister.getPduPersister(mContext);
        GenericPdu pdu = p.load(mMessageUri);

        if (pdu.getMessageType() != PduHeaders.MESSAGE_TYPE_SEND_REQ) {
            throw new MmsException("Invalid message: " + pdu.getMessageType());
        }

        SendReq sendReq = (SendReq) pdu;

        // Update headers.
        try {
			updatePreferencesHeaders(sendReq);
		} catch (InvalidHeaderValueException e) {
			Log.d(TAG,e.getMessage());
		}

        // MessageClass.
        sendReq.setMessageClass(DEFAULT_MESSAGE_CLASS.getBytes());

        // Update the 'date' field of the message before sending it.
        sendReq.setDate(System.currentTimeMillis() / 1000L);
        
        sendReq.setMessageSize(mMessageSize);

        p.updateHeaders(mMessageUri, sendReq);

        // Move the message into MMS Outbox
        p.move(mMessageUri, TMms.TOutbox.CONTENT_URI);

        // Start MMS transaction service
        SendingProgressTokenManager.put(ContentUris.parseId(mMessageUri), token);
        mContext.startService(new Intent(mContext, TransactionService.class));

        return true;
    }

    // Update the headers which are stored in SharedPreferences.
    private void updatePreferencesHeaders(SendReq sendReq) throws MmsException, InvalidHeaderValueException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Expiry.
        sendReq.setExpiry(prefs.getLong(
                MessagingPreferenceActivity.EXPIRY_TIME, DEFAULT_EXPIRY_TIME));

        // Priority.
        sendReq.setPriority(prefs.getInt(MessagingPreferenceActivity.PRIORITY, DEFAULT_PRIORITY));

        // Delivery report.
        boolean dr = prefs.getBoolean(MessagingPreferenceActivity.MMS_DELIVERY_REPORT_MODE,
                DEFAULT_DELIVERY_REPORT_MODE);
        sendReq.setDeliveryReport(dr?PduHeaders.VALUE_YES:PduHeaders.VALUE_NO);

        // Read report.
        boolean rr = prefs.getBoolean(MessagingPreferenceActivity.READ_REPORT_MODE,
                DEFAULT_READ_REPORT_MODE);
        sendReq.setReadReport(rr?PduHeaders.VALUE_YES:PduHeaders.VALUE_NO);
    }

    public static void sendReadRec(Context context, String to, String messageId, int status) {
        EncodedStringValue[] sender = new EncodedStringValue[1];
        sender[0] = new EncodedStringValue(to);

        try {
            final ReadRecInd readRec = new ReadRecInd(
                    new EncodedStringValue(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR.getBytes()),
                    messageId.getBytes(),
                    PduHeaders.CURRENT_MMS_VERSION,
                    status,
                    sender);

            readRec.setDate(System.currentTimeMillis() / 1000);

            PduPersister.getPduPersister(context).persist(readRec, TMms.TOutbox.CONTENT_URI);
            context.startService(new Intent(context, TransactionService.class));
        } catch (InvalidHeaderValueException e) {
            Log.e(TAG, "Invalide header value", e);
        } catch (MmsException e) {
            Log.e(TAG, "Persist message failed", e);
        }
    }
}
