package cn.edu.tsinghua.hpc.tcontacts.util;

import android.net.Uri;

public class TCallLog {
	public static final String AUTHORITY = "tcall_log";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	public static final class TCalls {
		 public static final Uri CONTENT_URI =
             Uri.parse("content://tcall_log/calls");
	}

}
