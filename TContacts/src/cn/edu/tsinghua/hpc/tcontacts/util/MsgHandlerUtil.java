package cn.edu.tsinghua.hpc.tcontacts.util;

import android.os.Handler;
import android.os.Message;

/**
 * 
 * @author zhangbing@inpurworld.com
 * MsgHandlerUtil is used to deal with handler msg dispatch.
 *
 */
public final class MsgHandlerUtil {

	public static void sendMsgToHandler(Handler handler,int what){
		Message msg = handler.obtainMessage();
		msg.what= what;
		handler.sendMessage(msg);
	}
}
