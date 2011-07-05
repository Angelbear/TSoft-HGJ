package cn.edu.tsinghua.hpc.tcontacts;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import cn.edu.tsinghua.hpc.tcontacts.pim.ContactStruct;
import cn.edu.tsinghua.hpc.tcontacts.pim.VCardComposer;
import cn.edu.tsinghua.hpc.tcontacts.pim.VCardConfig;
import cn.edu.tsinghua.hpc.tcontacts.pim.VCardDataBuilder;
import cn.edu.tsinghua.hpc.vcard.VCardParser;
import cn.edu.tsinghua.hpc.vcard.VCardParser_V21;

public class ImportContactsFromSystemActivity extends Activity {
	private static final String LOG_TAG = "ImportContactsFromSystemAcvitity";
	private Handler mHandler = new Handler();
	// String for storing error reason temporaly.
	private String mErrorReason;
	private ProgressDialog mProgressDialog;
	private ActualExportThread mActualExportThread;

	private static final int PROGRESS_DIALOG = 0;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		startImportContactsFromSystemActivity();
	}

	private void startImportContactsFromSystemActivity() {
		showDialog(R.id.dialog_export_confirmation);
	}

	private class CancelListener implements DialogInterface.OnClickListener,
			DialogInterface.OnCancelListener {
		public void onClick(DialogInterface dialog, int which) {
			finish();
		}

		public void onCancel(DialogInterface dialog) {
			finish();
		}
	}

	private CancelListener mCancelListener = new CancelListener();

	public class HandlerForOutputStream implements
			cn.edu.tsinghua.hpc.vcard.VCardComposer.OneEntryHandler {

		@Override
		public boolean onEntryCreated(String vcard) {
			VCardDataBuilder builder;
			try {
				builder = new VCardDataBuilder("UTF-8", "UTF-8", false,
						VCardConfig.VCARD_TYPE_V21_GENERIC, null);
				VCardParser p = new VCardParser_V21();
				p.parse(new ByteArrayInputStream(vcard.getBytes()), "UTF-8",
						builder);
				ContactStruct contact = builder.mLastContactStruct;
				contact.pushIntoContentResolver(getContentResolver());
			} catch (Exception e) {
				Log.d(LOG_TAG, e.getMessage());
			}

			return true;
		}

		@Override
		public boolean onInit(Context context) {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public void onTerminate() {
			// TODO Auto-generated method stub

		}
	}

	private class ErrorReasonDisplayer implements Runnable {
		private final int mResId;

		public ErrorReasonDisplayer(int resId) {
			mResId = resId;
		}

		public ErrorReasonDisplayer(String errorReason) {
			mResId = R.id.dialog_fail_to_export_with_reason;
			mErrorReason = errorReason;
		}

		public void run() {
			// Show the Dialog only when the parent Activity is still alive.
			if (!ImportContactsFromSystemActivity.this.isFinishing()) {
				showDialog(mResId);
			}
		}
	}

	private String translateComposerError(String errorMessage) {
		Resources resources = getResources();
		if (VCardComposer.FAILURE_REASON_FAILED_TO_GET_DATABASE_INFO
				.equals(errorMessage)) {
			return resources
					.getString(R.string.composer_failed_to_get_database_infomation);
		} else if (VCardComposer.FAILURE_REASON_NO_ENTRY.equals(errorMessage)) {
			return resources
					.getString(R.string.composer_has_no_exportable_contact);
		} else if (VCardComposer.FAILURE_REASON_NOT_INITIALIZED
				.equals(errorMessage)) {
			return resources.getString(R.string.composer_not_initialized);
		} else {
			return errorMessage;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.dialog_export_confirmation: {
			return getExportConfirmationDialog();
		}
		case R.string.fail_reason_too_many_vcard: {
			return new AlertDialog.Builder(this).setTitle(
					R.string.exporting_contact_failed_title).setMessage(
					getString(R.string.exporting_contact_failed_message,
							getString(R.string.fail_reason_too_many_vcard)))
					.setPositiveButton(android.R.string.ok, mCancelListener)
					.create();
		}
		case R.id.dialog_fail_to_export_with_reason: {
			return getErrorDialogWithReason();
		}

		case PROGRESS_DIALOG: {
			return getExportingVCardDialog();
		}
		}

		return super.onCreateDialog(id);
	}

	private class ExportConfirmationListener implements
			DialogInterface.OnClickListener {

		public ExportConfirmationListener() {
		}

		public void onClick(DialogInterface dialog, int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				mActualExportThread = new ActualExportThread();
				mActualExportThread.start();
				showDialog(PROGRESS_DIALOG);
			}
		}
	}

	public Dialog getExportConfirmationDialog() {
		return new AlertDialog.Builder(this).setTitle(
				R.string.confirm_import_title).setMessage(
				getString(R.string.confirm_import_from_system))
				.setPositiveButton(android.R.string.ok,
						new ExportConfirmationListener()).setNegativeButton(
						android.R.string.cancel, mCancelListener)
				.setOnCancelListener(mCancelListener).create();
	}

	public Dialog getErrorDialogWithReason() {
		if (mErrorReason == null) {
			Log.e(LOG_TAG, "Error reason must have been set.");
			mErrorReason = getString(R.string.fail_reason_unknown);
		}
		return new AlertDialog.Builder(this).setTitle(
				R.string.exporting_contact_failed_title).setMessage(
				getString(R.string.exporting_contact_failed_message,
						mErrorReason)).setPositiveButton(android.R.string.ok,
				mCancelListener).setOnCancelListener(mCancelListener).create();
	}

	public String getErrorReason() {
		return mErrorReason;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (id == R.id.dialog_fail_to_export_with_reason) {
			((AlertDialog) dialog).setMessage(getErrorReason());
		} else if (id == R.id.dialog_export_confirmation) {
			((AlertDialog) dialog)
					.setMessage(getString(R.string.confirm_import_from_system));
		} else {
			super.onPrepareDialog(id, dialog);
		}
	}

	private Dialog getExportingVCardDialog() {
		if (mProgressDialog == null) {
			String title = getString(R.string.importing_system_message);
			String message = getString(R.string.import_from_system);
			mProgressDialog = new ProgressDialog(
					ImportContactsFromSystemActivity.this);
			mProgressDialog.setTitle(title);
			mProgressDialog.setMessage(message);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setOnCancelListener(mActualExportThread);
		}
		return mProgressDialog;
	}

	private class ActualExportThread extends Thread implements
			DialogInterface.OnCancelListener {
		private PowerManager.WakeLock mWakeLock;
		private boolean mCanceled = false;

		public ActualExportThread() {
			PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = powerManager.newWakeLock(
					PowerManager.SCREEN_DIM_WAKE_LOCK
							| PowerManager.ON_AFTER_RELEASE, LOG_TAG);
		}

		@Override
		public void run() {
			boolean shouldCallFinish = true;
			mWakeLock.acquire();
			cn.edu.tsinghua.hpc.vcard.VCardComposer composer = null;
			try {
				composer = new cn.edu.tsinghua.hpc.vcard.VCardComposer(
						ImportContactsFromSystemActivity.this,
						getString(R.string.config_import_vcard_type), true);
				/*
				 * int vcardType = (VCardConfig.VCARD_TYPE_V21_GENERIC |
				 * VCardConfig.FLAG_USE_QP_TO_PRIMARY_PROPERTIES); composer =
				 * new VCardComposer(ExportVCardActivity.this, vcardType, true);
				 */

				composer.addHandler(new HandlerForOutputStream());

				if (!composer.init()) {
					final String errorReason = composer.getErrorReason();
					Log.e(LOG_TAG, "initialization of vCard composer failed: "
							+ errorReason);
					final String translatedErrorReason = translateComposerError(errorReason);
					mHandler.post(new ErrorReasonDisplayer(getString(
							R.string.fail_reason_could_not_initialize_exporter,
							translatedErrorReason)));
					shouldCallFinish = false;
					return;
				}

				int size = composer.getCount();

				if (size == 0) {
					mHandler
							.post(new ErrorReasonDisplayer(
									getString(R.string.fail_reason_no_exportable_contact)));
					shouldCallFinish = false;
					return;
				}

				try {

					Method setProgressNumberFormatMethod = Class.forName(
							"android.app.ProgressDialog").getMethod(
							"setProgressNumberFormat",
							new Class[] { String.class });
					setProgressNumberFormatMethod.setAccessible(true);
					setProgressNumberFormatMethod
							.invoke(
									mProgressDialog,
									new Object[] { getString(R.string.exporting_contact_list_progress) });
				} catch (Exception e) {
					Log.d("TContact", e.getMessage());
				}

				// mProgressDialog.setProgressNumberFormat(
				// getString(R.string.exporting_contact_list_progress));
				mProgressDialog.setMax(size);
				mProgressDialog.setProgress(0);

				while (!composer.isAfterLast()) {
					if (mCanceled) {
						return;
					}
					if (!composer.createOneEntry()) {
						final String errorReason = composer.getErrorReason();
						Log.e(LOG_TAG, "Failed to read a contact: "
								+ errorReason);
						final String translatedErrorReason = translateComposerError(errorReason);
						mHandler
								.post(new ErrorReasonDisplayer(
										getString(
												R.string.fail_reason_error_occurred_during_export,
												translatedErrorReason)));
						shouldCallFinish = false;
						return;
					}
					mProgressDialog.incrementProgressBy(1);
				}
			} finally {
				if (composer != null) {
					composer.terminate();
				}
				mWakeLock.release();
				mProgressDialog.dismiss();
				if (shouldCallFinish && !isFinishing()) {
					finish();
				}
			}
		}

		@Override
		public void finalize() {
			if (mWakeLock != null && mWakeLock.isHeld()) {
				mWakeLock.release();
			}
		}

		public void cancel() {
			mCanceled = true;
		}

		public void onCancel(DialogInterface dialog) {
			cancel();
		}
	}

}
