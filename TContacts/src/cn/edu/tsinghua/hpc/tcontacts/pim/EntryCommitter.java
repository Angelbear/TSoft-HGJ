package cn.edu.tsinghua.hpc.tcontacts.pim;

import android.content.ContentResolver;
import android.util.Log;

public class EntryCommitter implements EntryHandler {
    public static String LOG_TAG = "vcard.EntryComitter";

    private ContentResolver mContentResolver;
    private long mTimeToCommit;
    
    public EntryCommitter(ContentResolver resolver) {
        mContentResolver = resolver;
    }

    public void onParsingStart() {
    }
    
    public void onParsingEnd() {
        if (VCardConfig.showPerformanceLog()) {
            Log.d(LOG_TAG, String.format("time to commit entries: %d ms", mTimeToCommit));
        }
    }

    public void onEntryCreated(final ContactStruct contactStruct) {
        long start = System.currentTimeMillis();
        contactStruct.pushIntoContentResolver(mContentResolver);
        mTimeToCommit += System.currentTimeMillis() - start;
    }
}