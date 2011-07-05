/**
 * 
 */
package cn.edu.tsinghua.hpc.tmms;

import java.io.UnsupportedEncodingException;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Config;
import android.util.Log;

import com.ccit.phone.CCITSC;
import com.ccit.phone.LoginView;

/**
 * @author u
 * 
 */
class RequestLogin {

	private boolean mLogging = false;
	private boolean mCanceled = false;
	private Context mContext = null;
	private CCITSC mCCIT = null;
	// private CCITCrypto mCCIT = null;
	private OnLoginListener mLoginListener = null;
//	 private RequestLoginThread mLoginThread = null;
	private Handler mLoginHandler;
	private Handler mHandler;

	private static final String TAG = "RequestLogin";
	private static final boolean DEBUG = false;
	private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

	private LoginView login = null;
	private String strMsg;
	private static String token;
	private static String uid;

	public RequestLogin(Context context) {
		assert (context != null);
		mContext = context;
	}

	public boolean login(OnLoginListener listener) {

		mLoginHandler = new Handler() {
			public void handleMessage(Message msg) {
				if (mLogging) {// 没有被取消

					mLogging = false;
					UserInfo user = null;

					if (msg.arg1 == OnLoginListener.SERVER_AUTHEN_COMPLETED) {
						user = (UserInfo) msg.obj;

						UserManager.getInstance().setUser(user);
						if (mLoginListener != null) {
							// mLoginListener.OnLoginCompleted(user);
							mLoginListener.OnLogin(user);
						}
					} else {
						String message;
						try {
							message = new String((msg.getData().getString(
									"message").getBytes("utf-8")), "gbk");
							if (mLoginListener != null) {
								mLoginListener.OnLoginFailed(msg.arg1, message);
							}
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

				}
			}
		};
//		mHandler = new Handler() {
//			public void handleMessage(Message msg) {
//				switch (msg.what) {
//				case CCITSC.UI_HANDLER_SUCCESS_ID:
//					login = (LoginView) msg.obj;
//
//					if (login == null) {
//
//						strMsg = "LoginView is NULL";
//					} else {
//						strMsg = "LoginView is not NULL! UID:" + login.getUID();
//						uid = login.getUID();
//						token = new String(Base64.encode(login.getSignature()));
//						Log.d(TAG,"token:-----------"+token);
//						
//					}
//
//					break;
//				case CCITSC.UI_HANDLER_ERROR_ID:
//					
//				default:
//					strMsg = "登录请求错误!错误描述:" + (String) msg.obj;
//
//					break;
//
//				}
//				Log.d(TAG, strMsg + "----------------");
//			}
//
//		};

		if (mLogging) {
			if (LOCAL_LOGV) {
				Log.v(TAG, "RequestLogin is already logging.");
			}
			return false;
		}

		

		mLogging = true;
		mCanceled = false;
		mLoginListener = listener;

//		 mLoginThread = new RequestLoginThread();
//		 mLoginThread.start();
//		runlogin();
		return true;
	}

	public boolean cancel() {
		// if (!mLogging || mLoginThread == null) {
		// if (LOCAL_LOGV) {
		// Log.v(TAG, "RequestLogin is not logging.");
		// }
		// return false;
		// }
		//
		// if (!mLoginThread.isAlive() || mLoginThread.isInterrupted()) {
		// if (LOCAL_LOGV) {
		// Log.v(TAG, "cancel doesn't need.");
		// }
		// return false;
		// }
		//
		// mCanceled = true;
		// mLoginThread.interrupt();

		return true;
	}

//	 private class RequestLoginThread extends Thread {

	// @Override
//	public void runlogin() {
//
//		// this.setName("RequestLoginThread");
//		String message = null;
//
//		UserInfo user = null;
//		int total = -1;
//		int statusCode = OnLoginListener.INIT_LOGIN_COMPLETED;
//
//		// String token ="xxxweourewojfs"; //test
//		// String uid = "33"; //test
//		// HttpCommunication.initHttpParameter(MmsConfig.TOKEN, token);// test
//		// HttpCommunication.initHttpParameter(MmsConfig.UID, uid);// test
//		// user = new UserInfo(uid, token, total);//test
//		
//		try {
//			mCCIT = new CCITSC(mContext, "211.139.191.207", "8080", mHandler);
//			// mCCIT = new CCITCrypto(mContext, "211.139.191.207", "8080");
//		} catch (Exception e) {
//			e.printStackTrace();
//			statusCode = OnLoginListener.REQUEST_LOGIN_FAILED;
//			message = e.getMessage();
//		}
//		
//		try {
//
//			mCCIT.loginInit(false);
//
//		} catch (InterruptedException e) {
//			statusCode = OnLoginListener.REQUEST_LOGIN_INTERRUPTED;
//			message = e.getMessage();
//		} catch (Exception e) {
//			e.printStackTrace();
//			statusCode = OnLoginListener.REQUEST_LOGIN_FAILED;
//			message = e.getMessage();
//		}
//
//		if (statusCode == OnLoginListener.INIT_LOGIN_COMPLETED && !mCanceled) {
//
//			statusCode = OnLoginListener.REQUEST_LOGIN_COMPLETED;
//
//			try {
//
//				mCCIT.requestLogin(false);
//
//			} catch (InterruptedException e) {
//				statusCode = OnLoginListener.REQUEST_LOGIN_INTERRUPTED;
//				message = e.getMessage();
//			} catch (Exception e) {
//				e.printStackTrace();
//				statusCode = OnLoginListener.REQUEST_LOGIN_FAILED;
//				message = e.getMessage();
//			}
//
//		}
//
//		if (statusCode == OnLoginListener.REQUEST_LOGIN_COMPLETED && !mCanceled) {
//
//			statusCode = OnLoginListener.SERVER_AUTHEN_COMPLETED;
//
//			try {
//				Log.d(TAG, token+"----------"+uid);
//				HttpCommunication.initHttpParameter(MmsConfig.TOKEN, token);
//				HttpCommunication.initHttpParameter(MmsConfig.UID,
//						uid);
//
//				// HttpCommunication.initHttpParameter(MmsConfig.UID, uid);//
//				// test
//
//				total = SyncAction.getCountSMS(mContext,uid );
//				// total = SyncAction.getCountSMS(mContext, uid); //test
//
//				user = new UserInfo(uid, token, total);
//				// user = new UserInfo(uid, token, total);//test
//
//			} catch (ClientProtocolException cpe) {
//				statusCode = OnLoginListener.SERVER_AUTHEN_FAILED;
//				message = cpe.getMessage();
//			} catch (IOException ioe) {
//				statusCode = OnLoginListener.SERVER_AUTHEN_FAILED;
//				message = ioe.getMessage();
//			} catch (ServerActionFailed safe) {
//				statusCode = OnLoginListener.SERVER_AUTHEN_FAILED;
//				message = safe.getMessage();
//			} catch (ElementNotFound enfe) {
//				statusCode = OnLoginListener.SERVER_AUTHEN_FAILED;
//				message = enfe.getMessage();
//			} catch (DOMException dome) {
//				statusCode = OnLoginListener.SERVER_AUTHEN_FAILED;
//				message = dome.getMessage();
//			} catch (Exception e) {
//				e.printStackTrace();
//				statusCode = OnLoginListener.SERVER_AUTHEN_FAILED;
//				message = e.getMessage();
//			}
//		}
//
//		Message msg = mLoginHandler.obtainMessage();
//		msg.arg1 = mCanceled ? OnLoginListener.REQUEST_LOGIN_CANCED
//				: statusCode;
//		mCanceled = false;
//		if (user != null) {
//			msg.obj = user;
//		}
//		if (message != null) {
//			msg.getData().putString("message", message);
//		}
//		mLoginHandler.sendMessage(msg);
//	}
//	 }
}
