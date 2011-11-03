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

package cn.edu.tsinghua.hpc.tcontacts.ui.widget;

import java.util.ArrayList;

import android.content.Context;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.RawContacts;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import cn.edu.tsinghua.hpc.tcontacts.R;
import cn.edu.tsinghua.hpc.tcontacts.model.ContactsSource;
import cn.edu.tsinghua.hpc.tcontacts.model.ContactsSource.DataKind;
import cn.edu.tsinghua.hpc.tcontacts.model.Editor.EditorListener;
import cn.edu.tsinghua.hpc.tcontacts.model.EntityDelta;
import cn.edu.tsinghua.hpc.tcontacts.model.EntityDelta.ValuesDelta;
import cn.edu.tsinghua.hpc.tcontacts.model.EntityModifier;
import cn.edu.tsinghua.hpc.tcontacts.ui.ViewIdGenerator;

/**
 * Custom view that displays read-only contacts in the edit screen.
 */
class ReadOnlyContactEditorView extends BaseContactEditorView {

    private View mPhotoStub;
    private TextView mName;
    private TextView mReadOnlyWarning;
    private ViewGroup mGeneral;

    private View mHeaderColorBar;
    private View mSideBar;
    private ImageView mHeaderIcon;
    private TextView mHeaderAccountType;
    private TextView mHeaderAccountName;

    private long mRawContactId = -1;

    public ReadOnlyContactEditorView(Context context) {
        super(context);
    }

    public ReadOnlyContactEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** {@inheritDoc} */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mInflater = (LayoutInflater)getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        mPhoto = (PhotoEditorView)findViewById(R.id.edit_photo);
        mPhotoStub = findViewById(R.id.stub_photo);

        mName = (TextView) findViewById(R.id.read_only_name);
        mReadOnlyWarning = (TextView) findViewById(R.id.read_only_warning);
        mGeneral = (ViewGroup)findViewById(R.id.sect_general);

        mHeaderColorBar = findViewById(R.id.header_color_bar);
        mSideBar = findViewById(R.id.color_bar);
        mHeaderIcon = (ImageView) findViewById(R.id.header_icon);
        mHeaderAccountType = (TextView) findViewById(R.id.header_account_type);
        mHeaderAccountName = (TextView) findViewById(R.id.header_account_name);
    }

    /**
     * Set the internal state for this view, given a current
     * {@link EntityDelta} state and the {@link ContactsSource} that
     * apply to that state.
     *
     * TODO: make this more generic using data from the source
     */
    @Override
    public void setState(EntityDelta state, ContactsSource source, ViewIdGenerator vig) {
        // Remove any existing sections
        mGeneral.removeAllViews();

        // Bail if invalid state or source
        if (state == null || source == null) return;

        // Make sure we have StructuredName
        EntityModifier.ensureKindExists(state, source, StructuredName.CONTENT_ITEM_TYPE);

        // Fill in the header info
        ValuesDelta values = state.getValues();
        String accountName = values.getAsString(RawContacts.ACCOUNT_NAME);
        CharSequence accountType = source.getDisplayLabel(getContext());
        if (TextUtils.isEmpty(accountType)) {
            accountType = getContext().getString(R.string.account_phone);
        }
        if (!TextUtils.isEmpty(accountName)) {
            mHeaderAccountName.setText(
            		getContext().getString(R.string.from_account_format, accountName));
        }
        mHeaderAccountType.setText(getContext().getString(R.string.account_type_format, accountType));
        mHeaderIcon.setImageDrawable(source.getDisplayIcon(getContext()));

        mRawContactId = values.getAsLong(RawContacts._ID);

        ValuesDelta primary;

        // Photo
        DataKind kind = source.getKindForMimetype(Photo.CONTENT_ITEM_TYPE);
        if (kind != null) {
            EntityModifier.ensureKindExists(state, source, Photo.CONTENT_ITEM_TYPE);
            mHasPhotoEditor = (source.getKindForMimetype(Photo.CONTENT_ITEM_TYPE) != null);
            primary = state.getPrimaryEntry(Photo.CONTENT_ITEM_TYPE);
            mPhoto.setValues(kind, primary, state, source.readOnly, vig);
            if (!mHasPhotoEditor || !mPhoto.hasSetPhoto()) {
                mPhotoStub.setVisibility(View.GONE);
            } else {
                mPhotoStub.setVisibility(View.VISIBLE);
            }
        } else {
            mPhotoStub.setVisibility(View.VISIBLE);
        }

        // Name
        primary = state.getPrimaryEntry(StructuredName.CONTENT_ITEM_TYPE);
        mName.setText(primary.getAsString(StructuredName.DISPLAY_NAME));

        // Read only warning
        mReadOnlyWarning.setText(getContext().getString(R.string.contact_read_only, accountType));

        // Phones
        ArrayList<ValuesDelta> phones = state.getMimeEntries(Phone.CONTENT_ITEM_TYPE);
        if (phones != null) {
            for (ValuesDelta phone : phones) {
                View field = mInflater.inflate(
                        R.layout.item_read_only_field, mGeneral, false);
                TextView v;
                v = (TextView) field.findViewById(R.id.label);
                v.setText(getContext().getText(R.string.phoneLabelsGroup));
                v = (TextView) field.findViewById(R.id.data);
                v.setText(PhoneNumberUtils.formatNumber(phone.getAsString(Phone.NUMBER)));
                mGeneral.addView(field);
            }
        }

        // Emails
        ArrayList<ValuesDelta> emails = state.getMimeEntries(Email.CONTENT_ITEM_TYPE);
        if (emails != null) {
            for (ValuesDelta email : emails) {
                View field = mInflater.inflate(
                        R.layout.item_read_only_field, mGeneral, false);
                TextView v;
                v = (TextView) field.findViewById(R.id.label);
                v.setText(getContext().getText(R.string.emailLabelsGroup));
                v = (TextView) field.findViewById(R.id.data);
                v.setText(email.getAsString(Email.DATA));
                mGeneral.addView(field);
            }
        }

        // Hide mGeneral if it's empty
        if (mGeneral.getChildCount() > 0) {
            mGeneral.setVisibility(View.VISIBLE);
        } else {
            mGeneral.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the {@link EditorListener} on the name field
     */
    @Override
    public void setNameEditorListener(EditorListener listener) {
        // do nothing
    }

    @Override
    public long getRawContactId() {
        return mRawContactId;
    }
}
