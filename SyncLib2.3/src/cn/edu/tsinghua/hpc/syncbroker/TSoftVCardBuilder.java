package cn.edu.tsinghua.hpc.syncbroker;

import android.accounts.Account;
import cn.edu.tsinghua.hpc.vcard.VCardDataBuilder;

public class TSoftVCardBuilder extends VCardDataBuilder {

	public TSoftVCardBuilder() {
		// TODO Auto-generated constructor stub
	}

	public TSoftVCardBuilder(int vcardType) {
		super(vcardType);
		// TODO Auto-generated constructor stub
	}

	public TSoftVCardBuilder(String charset, boolean strictLineBreakParsing,
			int vcardType, Account account) {
		super(charset, strictLineBreakParsing, vcardType, account);
		// TODO Auto-generated constructor stub
	}

	public TSoftVCardBuilder(String sourceCharset, String targetCharset,
			boolean strictLineBreakParsing, int vcardType, Account account) {
		super(sourceCharset, targetCharset, strictLineBreakParsing, vcardType,
				account);
		// TODO Auto-generated constructor stub
	}

}
