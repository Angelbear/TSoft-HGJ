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

package cn.edu.tsinghua.hpc.tmms;

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Handler;
import android.preference.PreferenceManager;
import cn.edu.tsinghua.hpc.tmms.data.Contact;
import cn.edu.tsinghua.hpc.tmms.data.Conversation;
import cn.edu.tsinghua.hpc.tmms.drm.DrmUtils;
import cn.edu.tsinghua.hpc.tmms.layout.LayoutManager;
import cn.edu.tsinghua.hpc.tmms.syncaction.MmsUtils;
import cn.edu.tsinghua.hpc.tmms.util.ContactInfoCache;
import cn.edu.tsinghua.hpc.tmms.util.DownloadManager;
import cn.edu.tsinghua.hpc.tmms.util.DraftCache;
import cn.edu.tsinghua.hpc.tmms.util.RateController;
import cn.edu.tsinghua.hpc.tmms.util.SmileyParser;
import cn.edu.tsinghua.hpc.tmms.util.TTelephony.TThreads;

public class MmsApp extends Application {
    public static final String LOG_TAG = "Mms";
    private MessagesContentObserver observer;
    @Override
    public void onCreate() {
        super.onCreate();
        observer = new MessagesContentObserver(new Handler());
        getContentResolver().registerContentObserver(TThreads.CONTENT_URI, false, observer);
        // Load the default preference values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        MmsConfig.init(this);
        UserManager.newInstance(this);
        ContactInfoCache.init(this);
        Contact.init(this);
        DraftCache.init(this);
        Conversation.init(this);
        DownloadManager.init(this);
        RateController.init(this);
        DrmUtils.cleanupStorage(this);
        LayoutManager.init(this);
        SmileyParser.init(this);
        
        MmsUtils.notifySyncService(this);
    }

    @Override
    public void onTerminate() {
    	getContentResolver().unregisterContentObserver(observer);
        DrmUtils.cleanupStorage(this);
        super.onTerminate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LayoutManager.getInstance().onConfigurationChanged(newConfig);
    }
    
    private class MessagesContentObserver extends ContentObserver {

		public MessagesContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			Intent in = new Intent();
			in.setAction("cn.tsinghua.hpc.TSYNC_REQUEST.MMS");
			sendBroadcast(in);
		}
	}
}
