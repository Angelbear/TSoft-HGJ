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

package cn.edu.tsinghua.hpc.tmms;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import cn.edu.tsinghua.hpc.tmms.ui.MessagingPreferenceActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Config;
import android.util.Log;

public class MmsConfig {
	private static Context mContext;
    private static final String TAG = "MmsConfig";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private static final String DEFAULT_HTTP_KEY_X_WAP_PROFILE = "x-wap-profile";
    private static final String DEFAULT_USER_AGENT = "Android-Mms/2.0";

    private static final int MAX_IMAGE_HEIGHT = 480;
    private static final int MAX_IMAGE_WIDTH = 640;

    /**
     * Whether to hide MMS functionality from the user (i.e. SMS only).
     */
    private static boolean mTransIdEnabled = false;
    private static int mMmsEnabled = 1;                         // default to true
    private static int mMaxMessageSize = 300 * 1024;            // default to 300k max size
    private static String mUserAgent = DEFAULT_USER_AGENT;
    private static String mUaProfTagName = DEFAULT_HTTP_KEY_X_WAP_PROFILE;
    private static String mUaProfUrl = null;
    private static String mHttpParams = null;
    private static String mHttpParamsLine1Key = null;
    private static String mEmailGateway = null;
    private static int mMaxImageHeight = MAX_IMAGE_HEIGHT;      // default value
    private static int mMaxImageWidth = MAX_IMAGE_WIDTH;        // default value
    private static int mRecipientLimit = Integer.MAX_VALUE;     // default value
    private static int mDefaultSMSMessagesPerThread = 200;      // default value
    private static int mDefaultMMSMessagesPerThread = 20;       // default value
    private static int mMinMessageCountPerThread = 2;           // default value
    private static int mMaxMessageCountPerThread = 5000;        // default value
    private static int mHttpSocketTimeout = 60*1000;            // default to 1 min
    private static int mMinimumSlideElementDuration = 7;        // default to 7 sec
    private static boolean mNotifyWapMMSC = false;
    private static boolean mAllowAttachAudio = true;
	private static String mIPOfUMP = null;
	private static String mPortOfUMP = null;

 	/**
	 * Add by Yangyang Zhao
	 */
	private static final String FIRST_LAUNCH = "firstLaunch";
	private static final String USERID = "userID";
	private static final String IMSI = "imsi";
	private static final String FIRST_SYNC = "firstSync";
	private static final String LAST_SYNC_TIME = "lastSyncTime";
	private static final String LAST_SYNC_STATUS = "lastSyncStatus";
	private static final String LAST_SYNC_CODE = "lastSyncCode";

	public static final String UID = "Uid";
	public static final String TOKEN = "Token";
	public static final String SESSIONID = "SessionId";

	public static int maxSmsNum = 300;
	public static int maxMmsNum = 20;
	public static int maxMessageNum = 300;

	public static void setFirstLaunch(boolean firstLaunch) {
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(FIRST_LAUNCH, firstLaunch);
		editor.commit();
	}

	public static boolean isFirstLaunch() {
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		boolean first = settings.getBoolean(FIRST_LAUNCH, true);
		return first;
	}

	public static void setFirstSync(boolean firstSync, Context ctx) {
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(FIRST_SYNC, firstSync);
		editor.commit();
		if (!firstSync)
			setIMSI(ctx);
	}

	public static void setIMSI(Context ctx) {
		Log.d(TAG, "setIMSI");
		TelephonyManager tm = (TelephonyManager) ctx
				.getSystemService(Context.TELEPHONY_SERVICE);
		String imsi = tm.getSubscriberId();

		if (imsi == null)
			imsi = "noSIM";

		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(IMSI, imsi);
		editor.commit();

	}

	public static String getIMSI() {
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		String imsi = settings.getString(IMSI, null);
		Log.d(TAG, "IMSI: " + imsi);
		return imsi;
	}

	public static boolean isFirstSync() {
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		boolean firstSync = settings.getBoolean(FIRST_SYNC, true);
		return firstSync;
	}

	// uid must be not null.
	public static int isUserNeedFirstSync(Context ctx, String uid) {
		String priorID = null; 
		String imsi = getIMSI(ctx); 
		String pro_uid = getUserID();
		
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		priorID = settings.getString(IMSI, "");
		Log.d(TAG, "priorID:" + priorID);
		// TelephonyManager tm = (TelephonyManager)
		// ctx.getSystemService(Context.TELEPHONY_SERVICE);
		// imsi = tm.getSubscriberId();
		// Log.d(TAG,"userID:"+imsi);

		// if(imsi==null || imsi.equals("")){
		// return 0;
		// }else{
		
		
		Log.d(TAG, "---------pro_uid------"+pro_uid);
		Log.d(TAG, "---------uid------"+uid);
		if (priorID == null || priorID.equals("noSIM") || imsi == null
				|| imsi.equals("")) {
			return 1;
		}
		else if (imsi.equals(priorID)) {
			if (pro_uid!=null && pro_uid.equals(uid)) {
				return 2;
			} else {
				return 1;
			}
		} else {
			return 1;
		}

		// }

		// if(priorID.equals(imsi) || (priorID.equals("noSIM") && imsi==null))
		// return false;
		// else if(imsi == null){
		// imsi = "noSIM";
		// SharedPreferences.Editor editor = settings.edit();
		// editor.putString(IMSI, imsi);
		// editor.commit();
		// //return true;
		// }
		//
		//
		// assert(uid != null && uid.length() > 0);
		// String userId = MmsConfig.getUserID();
		// if (userId == null || (userId != null && !uid.equals(uid))) {
		// return true;
		// }
		// return false;
	}

	public static String getIMSI(Context ctx) {
		TelephonyManager tm = (TelephonyManager) ctx
				.getSystemService(Context.TELEPHONY_SERVICE);
		String imsi = tm.getSubscriberId();//
		return imsi;
	}

	// private static String priorID = null;
	// private static String imsi = "";

	public static void setTSyncEnabled(boolean tsyncEnabled) {
		SharedPreferences sp = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		SharedPreferences.Editor editor = sp.edit();
		editor.putBoolean(MessagingPreferenceActivity.TRANSPARENT_SYNC,
				tsyncEnabled);
		editor.commit();
	}

	public static boolean isTSyncEnabled() {
		SharedPreferences sp = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		return sp
				.getBoolean(MessagingPreferenceActivity.TRANSPARENT_SYNC, true);
	}

	public static String getUserID() {
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		String userID = settings.getString(USERID, null);
		Log.d(TAG, "UserID: " + userID);
		return userID;
	}

	public static void setUserID(String userID) {
		Log.d(TAG, "setUserID");
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(USERID, userID);
		editor.commit();
	}

	public static String getToken() {
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		String userID = settings.getString(TOKEN, null);
		Log.d(TAG, "TOKEN: " + userID);
		return userID;
	}

	public static void setToken(String token) {
		Log.d(TAG, "setToken");
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(TOKEN, token);
		editor.commit();
	}

	public static void setLastSyncTime(Time time) {
		Log.d(TAG, "setLastSyncTime:" + time.toString());
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong(LAST_SYNC_TIME, time.toMillis(false));
		editor.commit();
	}

	public static Time getLastSyncTime() {
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		long millis = settings.getLong(LAST_SYNC_TIME, 0);
		Time time = null;
		if (millis != 0) {
			time = new Time();
			time.set(millis);
		}
		Log.d(TAG, "getLastSyncTime: " + time != null ? time.toString()
				: "none");
		return time;
	}

	public static void setLastSyncStatus(boolean status) {
		Log.d(TAG, "setLastSyncStatus: " + status);
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(LAST_SYNC_STATUS, status);
		editor.commit();
	}

	public static boolean getLastSyncStatus() {
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		boolean status = settings.getBoolean(LAST_SYNC_STATUS, true);
		Log.d(TAG, "getLastSyncStatus: " + status);
		return status;
	}

	public static void setLastSyncCode(int code) {
		Log.d(TAG, "setLastSyncCode: " + code);
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(LAST_SYNC_CODE, code);
		editor.commit();
	}

	public static int getLastSyncCode() {
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		int code = settings.getInt(LAST_SYNC_CODE, 0);
		Log.d(TAG, "getLastSyncCode: " + code);
		return code;
	}

	public static void setSessionID(String sessionID) {
		Log.d(TAG, "setSessionID: " + sessionID);
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(SESSIONID, sessionID);
		editor.commit();
	}

	public static String getSessionID() {
		SharedPreferences settings = mContext.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		String sessionID = settings.getString(SESSIONID, null);
		Log.d(TAG, "getSessionID: " + sessionID);
		return sessionID;
	}

	public static void wipedata(Context ctx) {
		SharedPreferences settings = ctx.getSharedPreferences(
				"cn.edu.tsinghua.hpc.tmms", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(FIRST_SYNC, true);
		editor.remove(USERID);
		editor.commit();
	} 
    
    // This is the max amount of storage multiplied by mMaxMessageSize that we
    // allow of unsent messages before blocking the user from sending any more
    // MMS's.
    private static int mMaxSizeScaleForPendingMmsAllowed = 4;       // default value

    // Email gateway alias support, including the master switch and different rules
    private static boolean mAliasEnabled = false;
    private static int mAliasRuleMinChars = 2;
    private static int mAliasRuleMaxChars = 48;

    public static void init(Context context) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "MmsConfig.init()");
        }
        mContext = context;
        loadMmsSettings(context);
    }

    public static boolean getMmsEnabled() {
        return mMmsEnabled == 1 ? true : false;
    }

    public static int getMaxMessageSize() {
        return mMaxMessageSize;
    }

    /**
     * This function returns the value of "enabledTransID" present in mms_config file.
     * In case of single segment wap push message, this "enabledTransID" indicates whether
     * TransactionID should be appended to URI or not.
     */
    public static boolean getTransIdEnabled() {
        return mTransIdEnabled;
    }

    public static String getUserAgent() {
        return mUserAgent;
    }

    public static String getUaProfTagName() {
        return mUaProfTagName;
    }

    public static String getUaProfUrl() {
        return mUaProfUrl;
    }

    public static String getHttpParams() {
        return mHttpParams;
    }

    public static String getHttpParamsLine1Key() {
        return mHttpParamsLine1Key;
    }

    public static String getEmailGateway() {
        return mEmailGateway;
    }

    public static int getMaxImageHeight() {
        return mMaxImageHeight;
    }

    public static int getMaxImageWidth() {
        return mMaxImageWidth;
    }

    public static int getRecipientLimit() {
        return mRecipientLimit;
    }

    public static int getDefaultSMSMessagesPerThread() {
        return mDefaultSMSMessagesPerThread;
    }

    public static int getDefaultMMSMessagesPerThread() {
        return mDefaultMMSMessagesPerThread;
    }

    public static int getMinMessageCountPerThread() {
        return mMinMessageCountPerThread;
    }

    public static int getMaxMessageCountPerThread() {
        return mMaxMessageCountPerThread;
    }

    public static int getHttpSocketTimeout() {
        return mHttpSocketTimeout;
    }

    public static int getMinimumSlideElementDuration() {
        return mMinimumSlideElementDuration;
    }

    public static boolean getNotifyWapMMSC() {
        return mNotifyWapMMSC;
    }

    public static int getMaxSizeScaleForPendingMmsAllowed() {
        return mMaxSizeScaleForPendingMmsAllowed;
    }

    public static boolean isAliasEnabled() {
        return mAliasEnabled;
    }

    public static int getAliasMinChars() {
        return mAliasRuleMinChars;
    }

    public static int getAliasMaxChars() {
        return mAliasRuleMaxChars;
    }

    public static boolean getAllowAttachAudio() {
        return mAllowAttachAudio;
    }
	public static String getIPOfUMP() {
		return mIPOfUMP;
	}

	public static String getPortOfUMP() {
		return mPortOfUMP;
	}
    public static final void beginDocument(XmlPullParser parser, String firstElementName) throws XmlPullParserException, IOException
    {
        int type;
        while ((type=parser.next()) != parser.START_TAG
                   && type != parser.END_DOCUMENT) {
            ;
        }

        if (type != parser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                    ", expected " + firstElementName);
        }
    }

    public static final void nextElement(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        int type;
        while ((type=parser.next()) != parser.START_TAG
                   && type != parser.END_DOCUMENT) {
            ;
        }
    }
    
    private static void loadMmsSettings(Context context) {
        XmlResourceParser parser = context.getResources().getXml(R.xml.mms_config);

        try {
            beginDocument(parser, "mms_config");

            while (true) {
                nextElement(parser);
                String tag = parser.getName();
                if (tag == null) {
                    break;
                }
                String name = parser.getAttributeName(0);
                String value = parser.getAttributeValue(0);
                String text = null;
                if (parser.next() == XmlPullParser.TEXT) {
                    text = parser.getText();
                }

                if (DEBUG) {
                    Log.v(TAG, "tag: " + tag + " value: " + value);
                }
                if ("name".equalsIgnoreCase(name)) {
                    if ("bool".equals(tag)) {
                        // bool config tags go here
                        if ("enabledMMS".equalsIgnoreCase(value)) {
                            mMmsEnabled = "true".equalsIgnoreCase(text) ? 1 : 0;
                        } else if ("enabledTransID".equalsIgnoreCase(value)) {
                            mTransIdEnabled = "true".equalsIgnoreCase(text);
                        } else if ("enabledNotifyWapMMSC".equalsIgnoreCase(value)) {
                            mNotifyWapMMSC = "true".equalsIgnoreCase(text);
                        } else if ("aliasEnabled".equalsIgnoreCase(value)) {
                            mAliasEnabled = "true".equalsIgnoreCase(text);
                        } else if ("allowAttachAudio".equalsIgnoreCase(value)) {
                            mAllowAttachAudio = "true".equalsIgnoreCase(text);
                        }
                    } else if ("int".equals(tag)) {
                        // int config tags go here
                        if ("maxMessageSize".equalsIgnoreCase(value)) {
                            mMaxMessageSize = Integer.parseInt(text);
                        } else if ("maxImageHeight".equalsIgnoreCase(value)) {
                            mMaxImageHeight = Integer.parseInt(text);
                        } else if ("maxImageWidth".equalsIgnoreCase(value)) {
                            mMaxImageWidth = Integer.parseInt(text);
                        } else if ("defaultSMSMessagesPerThread".equalsIgnoreCase(value)) {
                            mDefaultSMSMessagesPerThread = Integer.parseInt(text);
                        } else if ("defaultMMSMessagesPerThread".equalsIgnoreCase(value)) {
                            mDefaultMMSMessagesPerThread = Integer.parseInt(text);
                        } else if ("minMessageCountPerThread".equalsIgnoreCase(value)) {
                            mMinMessageCountPerThread = Integer.parseInt(text);
                        } else if ("maxMessageCountPerThread".equalsIgnoreCase(value)) {
                            mMaxMessageCountPerThread = Integer.parseInt(text);
                        } else if ("recipientLimit".equalsIgnoreCase(value)) {
                            mRecipientLimit = Integer.parseInt(text);
                            if (mRecipientLimit < 0) {
                                mRecipientLimit = Integer.MAX_VALUE;
                            }
                        } else if ("httpSocketTimeout".equalsIgnoreCase(value)) {
                            mHttpSocketTimeout = Integer.parseInt(text);
                        } else if ("minimumSlideElementDuration".equalsIgnoreCase(value)) {
                            mMinimumSlideElementDuration = Integer.parseInt(text);
                        } else if ("maxSizeScaleForPendingMmsAllowed".equalsIgnoreCase(value)) {
                            mMaxSizeScaleForPendingMmsAllowed = Integer.parseInt(text);
                        } else if ("aliasMinChars".equalsIgnoreCase(value)) {
                            mAliasRuleMinChars = Integer.parseInt(text);
                        } else if ("aliasMaxChars".equalsIgnoreCase(value)) {
                            mAliasRuleMaxChars = Integer.parseInt(text);
                        }
                    } else if ("string".equals(tag)) {
                        // string config tags go here
                        if ("userAgent".equalsIgnoreCase(value)) {
                            mUserAgent = text;
                        } else if ("uaProfTagName".equalsIgnoreCase(value)) {
                            mUaProfTagName = text;
                        } else if ("uaProfUrl".equalsIgnoreCase(value)) {
                            mUaProfUrl = text;
                        } else if ("httpParams".equalsIgnoreCase(value)) {
                            mHttpParams = text;
                        } else if ("httpParamsLine1Key".equalsIgnoreCase(value)) {
                            mHttpParamsLine1Key = text;
                        } else if ("emailGatewayNumber".equalsIgnoreCase(value)) {
                            mEmailGateway = text;
						} else if ("ipOfUMP".equalsIgnoreCase(value)) {
							mIPOfUMP = text;
						} else if ("portOfUMP".equalsIgnoreCase(value)) {
							mPortOfUMP = text;
                        }
                    }
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } catch (NumberFormatException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } catch (IOException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } finally {
            parser.close();
        }

        String errorStr = null;

        if (getMmsEnabled() && mUaProfUrl == null) {
            errorStr = "uaProfUrl";
        }

        if (errorStr != null) {
            String err =
                String.format("MmsConfig.loadMmsSettings mms_config.xml missing %s setting",
                        errorStr);
            Log.e(TAG, err);
            throw new ContentRestrictionException(err);
        }
    }

}
