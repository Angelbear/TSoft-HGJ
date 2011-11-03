package cn.edu.tsinghua.hpc.tcontacts.model;


//test
public class AccountTypeInfo {

    public static final String ACCOUNT_TYPE_PHONE = "cn.edut.tsinghua.hpc.tcontact.phone";
    public static final String ACCOUNT_TYPE_SIM = "cn.edut.tsinghua.hpc.tcontact.sim";
    public static final String ACCOUNT_TYPE_MYPROFILE = "cn.edut.tsinghua.hpc.tcontact.myprofile";
    public static final String ACCOUNT_TYPE_GOOGLE = "com.google";
    public static final String ACCOUNT_TYPE_EXCHANGE = "com.android.exchange";
    public static final String ACCOUNT_SELECT_GOOGLE_TALK = "Google Talk";
    private static AccountTypeInfo mAccountsType = null;

    private AccountTypeInfo() {
    }

    public static AccountTypeInfo getAccountTypeConstants() {
        if (mAccountsType == null) {
            mAccountsType = new AccountTypeInfo();
        }
        return mAccountsType;

    }






}
