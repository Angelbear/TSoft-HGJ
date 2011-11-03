package cn.edu.tsinghua.hpc.tcontacts.model;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.BaseTypes;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.view.inputmethod.EditorInfo;
import cn.edu.tsinghua.hpc.tcontacts.R;
import cn.edu.tsinghua.hpc.tcontacts.util.Lists;

public class SimSource extends ContactsSource {
    public static final String ACCOUNT_TYPE =AccountTypeInfo.ACCOUNT_TYPE_SIM;
    
    protected static final int FLAGS_GROUP = EditorInfo.TYPE_CLASS_TEXT;
    protected static final int FLAGS_RINGTONE = EditorInfo.TYPE_CLASS_TEXT;
    
    protected static final int FLAGS_PHONE = EditorInfo.TYPE_CLASS_PHONE;
    protected static final int FLAGS_EMAIL = EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
    protected static final int FLAGS_PERSON_NAME = EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS | EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME;
    protected static final int FLAGS_PHONETIC = EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_VARIATION_PHONETIC;
    protected static final int FLAGS_GENERIC_NAME = EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS;
    protected static final int FLAGS_NOTE = EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
    protected static final int FLAGS_WEBSITE = EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_VARIATION_URI;
    protected static final int FLAGS_POSTAL = EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS | EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS
            | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;

    public SimSource() {
        this.accountType = ACCOUNT_TYPE;
        this.titleRes = R.string.account_sim;
        this.iconRes = R.drawable.contacts_popup_icon_sim_icon;
        
    }
    public SimSource(String resPackageName) {
        this.accountType = ACCOUNT_TYPE;
        this.resPackageName = null;
        this.summaryResPackageName = resPackageName;
    }


    @Override
    protected void inflate(Context context, int inflateLevel) {

        inflateStructuredName(inflateLevel);
        inflatePhone(inflateLevel);
        /*
        if (PhoneBookManageSim.getSIMType().equals(PhoneBookManageSim.THREEG))
            inflateEmail(inflateLevel);
            */
        setInflatedLevel(inflateLevel);
    }

   
  
    protected EditType buildPhoneType(int type) {
        return new EditType(type, Phone.getTypeLabelResource(type));
    }
    
    protected EditType buildPhoneType(int type, int labelRes) {
        return new EditType(type, labelRes);
    }

    protected EditType buildEmailType(int type) {
        return new EditType(type, Email.getTypeLabelResource(type));
    }
    
    protected EditType buildPostalType(int type) {
        return new EditType(type, StructuredPostal.getTypeLabelResource(type));
    }

    protected EditType buildImType(int type) {
        return new EditType(type, Im.getProtocolLabelResource(type));
    }

    protected EditType buildOrgType(int type) {
        return new EditType(type, Organization.getTypeLabelResource(type));
    }

    protected DataKind inflateStructuredName(int inflateLevel) {
        DataKind kind = getKindForMimetype(StructuredName.CONTENT_ITEM_TYPE);
        if (kind == null) {
            kind = addKind(new DataKind(StructuredName.CONTENT_ITEM_TYPE, R.string.nameLabelsGroup,
                    -1, -1, true));
        }

        if (inflateLevel >= ContactsSource.LEVEL_CONSTRAINTS) {
            kind.fieldList = Lists.newArrayList();
            kind.fieldList.add(new EditField(StructuredName.GIVEN_NAME, R.string.name,
                    FLAGS_PERSON_NAME));
        }
        return kind;
    }

    protected DataKind inflatePhone(int inflateLevel) {
        DataKind kind = getKindForMimetype(Phone.CONTENT_ITEM_TYPE);
        if (kind == null) {
            kind = addKind(new DataKind(Phone.CONTENT_ITEM_TYPE, R.string.phoneLabelsGroup,
                    android.R.drawable.sym_action_call, 10, true));
            kind.actionBody = new SimpleInflater(Phone.NUMBER);
            kind.isList = false;
        }

        if (inflateLevel >= ContactsSource.LEVEL_CONSTRAINTS) {
            kind.typeColumn = Phone.TYPE;
            kind.fieldList = Lists.newArrayList();
            kind.fieldList.add(new EditField(Phone.NUMBER, R.string.phoneLabelsGroup, FLAGS_PHONE));
            kind.typeList = Lists.newArrayList();
            kind.typeList.add(buildPhoneType(Phone.TYPE_HOME).setSpecificMax(1));
        }
        return kind;
    }
    

    protected DataKind inflateEmail(int inflateLevel) {
        DataKind kind = getKindForMimetype(Email.CONTENT_ITEM_TYPE);
        if (kind == null) {
            kind = addKind(new DataKind(Email.CONTENT_ITEM_TYPE, R.string.emailLabelsGroup,
                    android.R.drawable.sym_action_email, 15, true));
            kind.actionHeader = new EmailActionInflater();
            kind.actionBody = new SimpleInflater(Email.DATA);
            kind.isList = false;
        }

        if (inflateLevel >= ContactsSource.LEVEL_CONSTRAINTS) {
            kind.typeColumn = Email.TYPE;
            kind.fieldList = Lists.newArrayList();
            kind.fieldList.add(new EditField(Email.DATA, R.string.emailLabelsGroup, FLAGS_EMAIL));
            kind.typeList = Lists.newArrayList();
            kind.typeList.add(buildEmailType(Email.TYPE_HOME).setSpecificMax(1));
        }

        return kind;
    }

    /**
     * Simple inflater that assumes a string resource has a "%s" that will be
     * filled from the given column.
     */
    public static class SimpleInflater implements StringInflater {
        private final int mStringRes;
        private final String mColumnName;

        public SimpleInflater(int stringRes) {
            this(stringRes, null);
        }

        public SimpleInflater(String columnName) {
            this(-1, columnName);
        }

        public SimpleInflater(int stringRes, String columnName) {
            mStringRes = stringRes;
            mColumnName = columnName;
        }

        public CharSequence inflateUsing(Context context, Cursor cursor) {
            final int index = mColumnName != null ? cursor.getColumnIndex(mColumnName) : -1;
            final boolean validString = mStringRes > 0;
            final boolean validColumn = index != -1;

            final CharSequence stringValue = validString ? context.getText(mStringRes) : null;
            final CharSequence columnValue = validColumn ? cursor.getString(index) : null;

            if (validString && validColumn) {
                return String.format(stringValue.toString(), columnValue);
            } else if (validString) {
                return stringValue;
            } else if (validColumn) {
                return columnValue;
            } else {
                return null;
            }
        }

        public CharSequence inflateUsing(Context context, ContentValues values) {
            final boolean validColumn = values.containsKey(mColumnName);
            final boolean validString = mStringRes > 0;

            final CharSequence stringValue = validString ? context.getText(mStringRes) : null;
            final CharSequence columnValue = validColumn ? values.getAsString(mColumnName) : null;

            if (validString && validColumn) {
                return String.format(stringValue.toString(), columnValue);
            } else if (validString) {
                return stringValue;
            } else if (validColumn) {
                return columnValue;
            } else {
                return null;
            }
        }
    }

    public static abstract class CommonInflater implements StringInflater {
        protected abstract int getTypeLabelResource(Integer type);

        protected boolean isCustom(Integer type) {
            return type == BaseTypes.TYPE_CUSTOM;
        }

        protected String getTypeColumn() {
            return Phone.TYPE;
        }

        protected String getLabelColumn() {
            return Phone.LABEL;
        }

        protected CharSequence getTypeLabel(Resources res, Integer type, CharSequence label) {
            final int labelRes = getTypeLabelResource(type);
            if (type == null) {
                return res.getText(labelRes);
            } else if (isCustom(type)) {
                return res.getString(labelRes, label == null ? "" : label);
            } else {
                return res.getText(labelRes);
            }
        }

        public CharSequence inflateUsing(Context context, Cursor cursor) {
            final Integer type = cursor.getInt(cursor.getColumnIndex(getTypeColumn()));
            final String label = cursor.getString(cursor.getColumnIndex(getLabelColumn()));
            return getTypeLabel(context.getResources(), type, label);
        }

        public CharSequence inflateUsing(Context context, ContentValues values) {
            final Integer type = values.getAsInteger(getTypeColumn());
            final String label = values.getAsString(getLabelColumn());
            return getTypeLabel(context.getResources(), type, label);
        }
    }

    public static class PhoneActionInflater extends CommonInflater {
        @Override
        protected boolean isCustom(Integer type) {
            return type == Phone.TYPE_CUSTOM || type == Phone.TYPE_ASSISTANT;
        }

        @Override
        protected int getTypeLabelResource(Integer type) {
            if (type == null) return R.string.call_other;
            switch (type) {
                case Phone.TYPE_HOME: return R.string.call_home;
                case Phone.TYPE_MOBILE: return R.string.call_mobile;
                case Phone.TYPE_WORK: return R.string.call_work;
                case Phone.TYPE_FAX_WORK: return R.string.call_fax_work;
                case Phone.TYPE_FAX_HOME: return R.string.call_fax_home;
                case Phone.TYPE_PAGER: return R.string.call_pager;
                case Phone.TYPE_OTHER: return R.string.call_other;
                case Phone.TYPE_CALLBACK: return R.string.call_callback;
                case Phone.TYPE_CAR: return R.string.call_car;
                case Phone.TYPE_COMPANY_MAIN: return R.string.call_company_main;
                case Phone.TYPE_ISDN: return R.string.call_isdn;
                case Phone.TYPE_MAIN: return R.string.call_main;
                case Phone.TYPE_OTHER_FAX: return R.string.call_other_fax;
                case Phone.TYPE_RADIO: return R.string.call_radio;
                case Phone.TYPE_TELEX: return R.string.call_telex;
                case Phone.TYPE_TTY_TDD: return R.string.call_tty_tdd;
                case Phone.TYPE_WORK_MOBILE: return R.string.call_work_mobile;
                case Phone.TYPE_WORK_PAGER: return R.string.call_work_pager;
                case Phone.TYPE_ASSISTANT: return R.string.call_assistant;
                case Phone.TYPE_MMS: return R.string.call_mms;
                default: return R.string.call_custom;
            }
        }
    }

    public static class PhoneActionAltInflater extends CommonInflater {
        @Override
        protected boolean isCustom(Integer type) {
            return (type == Phone.TYPE_CUSTOM || type == Phone.TYPE_ASSISTANT);
        }

        @Override
        protected int getTypeLabelResource(Integer type) {
            if (type == null) return R.string.sms_other;
            switch (type) {
                case Phone.TYPE_HOME: return R.string.sms_home;
                case Phone.TYPE_MOBILE: return R.string.sms_mobile;
                case Phone.TYPE_WORK: return R.string.sms_work;
                case Phone.TYPE_FAX_WORK: return R.string.sms_fax_work;
                case Phone.TYPE_FAX_HOME: return R.string.sms_fax_home;
                case Phone.TYPE_PAGER: return R.string.sms_pager;
                case Phone.TYPE_OTHER: return R.string.sms_other;
                case Phone.TYPE_CALLBACK: return R.string.sms_callback;
                case Phone.TYPE_CAR: return R.string.sms_car;
                case Phone.TYPE_COMPANY_MAIN: return R.string.sms_company_main;
                case Phone.TYPE_ISDN: return R.string.sms_isdn;
                case Phone.TYPE_MAIN: return R.string.sms_main;
                case Phone.TYPE_OTHER_FAX: return R.string.sms_other_fax;
                case Phone.TYPE_RADIO: return R.string.sms_radio;
                case Phone.TYPE_TELEX: return R.string.sms_telex;
                case Phone.TYPE_TTY_TDD: return R.string.sms_tty_tdd;
                case Phone.TYPE_WORK_MOBILE: return R.string.sms_work_mobile;
                case Phone.TYPE_WORK_PAGER: return R.string.sms_work_pager;
                case Phone.TYPE_ASSISTANT: return R.string.sms_assistant;
                case Phone.TYPE_MMS: return R.string.sms_mms;
                default: return R.string.sms_custom;
            }
        }
    }

    public static class EmailActionInflater extends CommonInflater {
        @Override
        protected int getTypeLabelResource(Integer type) {
            if (type == null) return R.string.email;
            switch (type) {
                case Email.TYPE_HOME: return R.string.email_home;
                case Email.TYPE_WORK: return R.string.email_work;
                case Email.TYPE_OTHER: return R.string.email_other;
                case Email.TYPE_MOBILE: return R.string.email_mobile;
                default: return R.string.email_custom;
            }
        }
    }

    public static class EventActionInflater extends CommonInflater {
        @Override
        protected int getTypeLabelResource(Integer type) {
            return Event.getTypeResource(type);
        }
    }

    @Override
    public int getHeaderColor(Context context) {
        return 0xff7f93bc;
    }

    @Override
    public int getSideBarColor(Context context) {
        return 0xffbdc7b8;
    }
}
