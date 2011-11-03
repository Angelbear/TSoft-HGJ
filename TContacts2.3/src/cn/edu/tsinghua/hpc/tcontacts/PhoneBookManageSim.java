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
package cn.edu.tsinghua.hpc.tcontacts;

import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.AdnRecord;
import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccConstants;

public class PhoneBookManageSim {
    
    private final static String TAG = "PhoneBookManageSim";
    
    private static Context mContext;
    private static TelephonyManager mTelMan;

    private final String NAME = "tag";
    private final String NUMBER = "number";
    private final String EMAIL = "emails";
    public static final Uri ADN_URI = Uri.parse("content://com.android.contacts/raw_contacts/adn");
    public static final Uri DATA_URI = Uri.parse("content://com.android.contacts/data/");
    public static final Uri RAWCONTACT_URI = Uri.parse("content://com.android.contacts/raw_contacts/");
    static IIccPhoneBook simPhoneBook = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
    public static List<AdnRecord> adnRecordList ;
    
    private static final String ICCTYPE = "ril.ICC_TYPE";
    private static final String TWOG = "1";
    public static final String THREEG = "2";
    
    static final String[] DATA_PROJECTION = new String[] { 
        "_id", 
        "mimetype",
        "raw_contact_id", 
        "is_primary", 
        "data1"
    };
    static final int DATA_ID_COLUMN_INDEX = 0;
    static final int DATA_MIMETYPE_COLUMN_INDEX = 1;
    static final int DATA_RAW_CONTACT_COLUMN_INDEX = 2;
    static final int DATA_IS_PRIMARY_COLUMN_INDEX = 3;
    static final int DATA_DATA1_COLUMN_INDEX = 4;

    private PhoneBookManageSim mPhoneBookManageSim = null;
    private static boolean isFirstCheck = true;
    private static String mSimType = null;
    private static int mCurrentSimState = -1;

    private static int mMaxRecordLength = -1;
    private static int mTotalEFFile = -1;
    private static int mNumberRecordsEFFile = -1;
    
    private static int mEmailMaxIndex = -1;
    private static int mEmailUsedIndex = -1;
    
    public final int COPY_SUCCESS = 1;
    public final int NULL_CONTACT_ERROR = -2;
    public final int WRITE_SIM_ERROR = -1;
    public static int mUsedCount = -1;
    public static int singleRecordLength = -1;
    public static int totalLength = -1;
    public static int numberOfRecords = -1;
    public static int singleRecordLengthEmail = -1;
    public static int totalLengthEmail = -1;
    public static int numberOfRecordsEmail = -1;
    private int mNameLength = 100;
    private int mNumberLength = 100;

    public PhoneBookManageSim getInstance(Context context) {
        
        if (mPhoneBookManageSim == null) {
            mPhoneBookManageSim = new PhoneBookManageSim(context);
        }
        return mPhoneBookManageSim;
    }
    
    public PhoneBookManageSim(Context context) {
        super();
        mContext = context;
    }
    
    public static boolean updateAdnRecordsInEfByIndex(int adnIndex, String alphaTag, String number) {
        String pin2 = null;
        int efid = IccConstants.EF_ADN;
        try {
            if (getSIMType().equals(THREEG)) {
                efid = IccConstants.EF_PBR;
            }
            AdnRecord firstAdn = new AdnRecord(alphaTag, number);
            boolean success = simPhoneBook.updateAdnRecordsInEfByIndex(efid,
                    firstAdn.getAlphaTag(), firstAdn.getNumber(), adnIndex, pin2);
            return success;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean updateAdnRecordsEmailInEfByIndex(String newEmail,
            int indexEmail, int adnIndex) {
        String pin2 = null;
        int efid = IccConstants.EF_PBR;
        try {
            if (getSIMType().equals(TWOG)) {
                return false;
            }
            boolean success = simPhoneBook.updateAdnRecordsEmailInEfByIndex(efid, newEmail,
                    indexEmail, adnIndex, pin2);
            return success;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isSimEnabled() {
        mTelMan = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if ((mTelMan.getSimState() == TelephonyManager.SIM_STATE_UNKNOWN)
                || (mTelMan.getSimState() == TelephonyManager.SIM_STATE_ABSENT)) {
            mCurrentSimState = mTelMan.getSimState();
            Log.d(TAG, "isSimEnabled() return false");
            return false;
        }
    
        if(mCurrentSimState != mTelMan.getSimState())
        {
            isFirstCheck = true;
            mCurrentSimState = mTelMan.getSimState();
        }
        
        return true;
    }
    public static boolean isSimDBReady() 
    {
        if(isFirstCheck)
        {
            isFirstCheck = false;
//            getSimStatus(mSimType);
        }
        Log.d(TAG, "isSimDBReady() : true");
        return true;
    
    }

    public static boolean hasIccCard() {
        boolean ret = false;
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (phone != null) {
                ret = phone.hasIccCard();
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        return ret;
    }
    
    public static void getAdnRecordsSize() 
    {
        try {
            int efid = IccConstants.EF_ADN;
            int efid2 = 0;
            if (getSIMType().equals(THREEG)){
                efid = IccConstants.EF_PBR;
                efid2 =  IccConstants.EF_PBR + 1;
            }
            int recordInfo[] = simPhoneBook.getAdnRecordsSize(efid);
            singleRecordLength = recordInfo[0];
            totalLength = recordInfo[1];
            numberOfRecords = recordInfo[2];
            if(efid2 != 0){
                int recordInfo2[] = simPhoneBook.getAdnRecordsSize(efid2);
                singleRecordLengthEmail = recordInfo2[0];
                totalLengthEmail = recordInfo2[1];
                numberOfRecordsEmail = recordInfo2[2];
            }
                
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public static int getNumberOfRecords(){
        return numberOfRecords;
    }
    public static int getSingleRecordLength(){
        return singleRecordLength;
    }

    public static String getSIMType() {
        String ret = null;
        try {
            if (simPhoneBook != null)
            	ret = "1";
                //ret = simPhoneBook.getSIMType();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
    
    public static int searchAdn2EmailIndex(int adn) {
        int ret = 0;
        try {
            if (simPhoneBook != null)
                ret = simPhoneBook.searchAdn2EmailIndex(adn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
    
    public static int getEmailRecordsSize(){
        int ret = 0;
        try {
            if (simPhoneBook != null)
                ret = simPhoneBook.getEmailRecordsSize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
}
