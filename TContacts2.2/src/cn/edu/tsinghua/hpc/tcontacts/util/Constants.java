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

package cn.edu.tsinghua.hpc.tcontacts.util;

import android.app.Service;
import android.provider.ContactsContract.CommonDataKinds.Phone;

/**
 * Background {@link Service} that is used to keep our process alive long enough
 * for background threads to finish. Started and stopped directly by specific
 * background tasks when needed.
 */
public class Constants {
    /**
     * Specific MIME-type for {@link Phone#CONTENT_ITEM_TYPE} entries that
     * distinguishes actions that should initiate a text message.
     */
    public static final String MIME_SMS_ADDRESS = "vnd.android.cursor.item/sms-address";

    public static final String SCHEME_TEL = "tel";
    public static final String SCHEME_SMSTO = "smsto";
    public static final String SCHEME_MAILTO = "mailto";
    public static final String SCHEME_IMTO = "imto";
    
    //error code. added by Boern
    public static final int ERROR_CODE_LOGIN_FAILURE = 0X01;
    public static final int ERROR_CODE_SYNC_FAILURE = 0X02;
    
    //bundle key.added by Boern
    public static final String KEY_ERROR_CODE = "loginFailure";
    public static final String KEY_ERROR_MSG = "errorMsg";
    public static final String KEY_SYNC_TYPE = "syncType";
    
    //
    public static final int SYNC_TYPE_FIRST = 0X00;
    public static final int SYNC_TYPE_RESYNC = 0X01;
    public static final int SYNC_TYPE_NOSYNC = 0X02;
    
    //login type
    public static final int LOGIN_TYPE_FIRST = 0X01;
    public static final int LOGIN_TYPE_RELOGIN= 0X02;
    public static final int LOGIN_TYPE_NORMAL = 0X03;
    
    public static final int LOGIN_SUCCESS = 0X04;
    public static final int LOGIN_FAILURE = 0X05;
    

}
