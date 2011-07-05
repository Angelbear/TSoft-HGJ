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


import java.util.List;

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import cn.edu.tsinghua.hpc.tcontacts.ImportContactsFromSystemActivity;
import cn.edu.tsinghua.hpc.tcontacts.ImportVCardActivity;
import cn.edu.tsinghua.hpc.tcontacts.R;
import cn.edu.tsinghua.hpc.tcontacts.model.ContactsSource;
import cn.edu.tsinghua.hpc.tcontacts.model.GoogleSource;
import cn.edu.tsinghua.hpc.tcontacts.model.Sources;

/**
 * Utility class for selectiong an Account for importing contact(s)
 */
public class AccountSelectionUtil {
    // TODO: maybe useful for EditContactActivity.java...
    private static final String LOG_TAG = "AccountSelectionUtil";

    private static class AccountSelectedListener
            implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

        final private Context mContext;
        final private List<Account> mAccountList;
        final private int mResId;

        public AccountSelectedListener(Context context, List<Account> accountList, int resId) {
            if (accountList == null || accountList.size() == 0) {
                Log.e(LOG_TAG, "The size of Account list is 0.");
            }
            mContext = context;
            mAccountList = accountList;
            mResId = resId;
        }

        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            doImport(mContext, mResId, mAccountList.get(which));
        }

        public void onCancel(DialogInterface dialog) {
            dialog.dismiss();
        }
    }

    public static Dialog getSelectAccountDialog(Context context, int resId) {
        return getSelectAccountDialog(context, resId, null);
    }

    public static Dialog getSelectAccountDialog(Context context, int resId,
            DialogInterface.OnCancelListener onCancelListener) {
        final Sources sources = Sources.getInstance(context);
        final List<Account> writableAccountList = sources.getAccounts(true);

        // Assume accountList.size() > 1

        // Wrap our context to inflate list items using correct theme
        final Context dialogContext = new ContextThemeWrapper(
                context, android.R.style.Theme_Light);
        final LayoutInflater dialogInflater = (LayoutInflater)dialogContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final ArrayAdapter<Account> accountAdapter =
            new ArrayAdapter<Account>(context, android.R.layout.simple_list_item_2,
                    writableAccountList) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = dialogInflater.inflate(
                            android.R.layout.simple_list_item_2,
                            parent, false);
                }

                // TODO: show icon along with title
                final TextView text1 =
                        (TextView)convertView.findViewById(android.R.id.text1);
                final TextView text2 =
                        (TextView)convertView.findViewById(android.R.id.text2);

                final Account account = this.getItem(position);
                final ContactsSource source =
                    sources.getInflatedSource(account.type,
                            ContactsSource.LEVEL_SUMMARY);
                final Context context = getContext();

                text1.setText(account.name);
                text2.setText(source.getDisplayLabel(context));

                return convertView;
            }
        };

        AccountSelectedListener accountSelectedListener =
            new AccountSelectedListener(context, writableAccountList, resId);
        return new AlertDialog.Builder(context)
            .setTitle(R.string.dialog_new_contact_account)
            .setSingleChoiceItems(accountAdapter, 0, accountSelectedListener)
            .setOnCancelListener(accountSelectedListener)
            .create();
    }

    public static void doImport(Context context, int resId, Account account) {
        switch (resId) {
            case R.string.import_from_sim: {
                doImportFromSim(context, account);
                break;
            }
            case R.string.import_from_sdcard: {
                doImportFromSdCard(context, account);
                break;
            }
            case R.string.import_from_system: {
            	doImportFromSystem(context, account);
            	break;
            }
        }
    }
    
    public static void doImportFromSystem(Context context, Account account) {
    	 if (account != null) {
             GoogleSource.createMyContactsIfNotExist(account, context);
         }
    	 
    	 // TODO:
    	 Intent importIntent = new Intent(context, ImportContactsFromSystemActivity.class);
         if (account != null) {
             importIntent.putExtra("account_name", account.name);
             importIntent.putExtra("account_type", account.type);
         }
         context.startActivity(importIntent);
    }
    
    public static void doImportFromSim(Context context, Account account) {
        if (account != null) {
            GoogleSource.createMyContactsIfNotExist(account, context);
        }

        Intent importIntent = new Intent(TIntent.ACTION_VIEW);
        importIntent.setType("vnd.android.cursor.item/sim-contact");
        if (account != null) {
            importIntent.putExtra("account_name", account.name);
            importIntent.putExtra("account_type", account.type);
        }
        importIntent.setClassName("com.android.phone", "com.android.phone.SimContacts");
        context.startActivity(importIntent);
    }

    public static void doImportFromSdCard(Context context, Account account) {
        if (account != null) {
            GoogleSource.createMyContactsIfNotExist(account, context);
        }

        Intent importIntent = new Intent(context, ImportVCardActivity.class);
        if (account != null) {
            importIntent.putExtra("account_name", account.name);
            importIntent.putExtra("account_type", account.type);
        }
        context.startActivity(importIntent);
    }
}
