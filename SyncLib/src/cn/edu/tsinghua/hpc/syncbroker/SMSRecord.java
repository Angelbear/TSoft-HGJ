package cn.edu.tsinghua.hpc.syncbroker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;


/**
 * NOTE: toVSMS() can export the current SMSRecord to vCard format, but we do
 * not have a corresponding importer, please use the Android vCard parser to get
 * each field, please implement postInit().
 * 
 * @author xrn
 * 
 */
public class SMSRecord extends SyncRecord {
	// inherit guid, tag, and data
	private String from;
	private String to;
	private Date date;
	private SMSType type = SMSType.RECEIVE;
	private SMSSubType subType = SMSSubType.FRIEND;
	private boolean read = true;
	private boolean secret = false;
	private String pdu = null;
	private String body = null;
	private MessageType mtype = MessageType.SMS;

	/**
	 * we now use vcard format to present vsms. Example:
	 * 
	 * BEGIN:VCARD 
	 * VERSION:2.1
	 * FROM:xxx
	 * TO:xxx
	 * TYPE:xxx
	 * SUBTYPE:xxx
	 * READ:[0|1]
	 * BODY;ENCODING=BASE64;CHARSET=UTF-8:xxx 
	 * MSG_TYPE: [mms|sms] 
	 * PDU;ENCODING=BASE64;CHARSET=UTF-8:xxx
	 * END:VCARD
	 * 
	 */
	private static final String vSMSTemplate = "BEGIN:VCARD\r\n" +
			"VERSION:2.1\r\n" + "N:Name;Faked\r\n" + // XXX: this is for vobject module
			"FN:Faked Name\r\n" + // XXX: this is for vobject module
			"%s:%s\r\n" + // from
			"%s:%s\r\n" + // to
			"%s:%d\r\n" + // date
			"%s:%s\r\n" + // type
			"%s:%s\r\n" + // subtype
			"%s:%d\r\n" + // read
			"%s;ENCODING=BASE64;CHARSET=UTF-8:%s\r\n" + // XXX: message body. vcard 2.1 uses BASE64, vcard 3.0 uses b
			"%s:%s\r\n" + // message type mms | sms
			"%s;ENCODING=BASE64;CHARSET=UTF-8:%s\r\n" + // XXX: pdu is pure binary, client should encode it to string first.
			"END:VCARD\r\n";

	/**
	 * @param guid
	 * @param tag
	 * @param data : the full vCard data.
	 */
	public SMSRecord(int guid, SyncTag tag, String data) {
		super(guid, tag, data);
		from = null;
		to = null;
		date = null;
		type = SMSType.UNKNOWN;
		subType = SMSSubType.UNKNOWN;
		secret = false;
		read = true;
		body = null;
		pdu = null;
		postInit();
	}

	/**
	 * @param from
	 * @param to
	 * @param date
	 * @param type
	 * @param subType
	 * @param secret
	 * @param body
	 */
	public SMSRecord(String from, String to, Date date, SMSType type,
			SMSSubType subType, boolean read, boolean secret, String body) {
		super();
		this.from = from;
		this.to = to;
		this.date = date;
		this.type = type;
		this.subType = subType;
		this.read = read;
		this.secret = secret;
		this.body = body;
		this.pdu = body;
		this.mtype = MessageType.SMS;
	}

	public SMSRecord(String from, String to, Date date, SMSType type,
			SMSSubType subType, boolean read, boolean secret, String body,
			String pdu) {
		super();
		this.from = from;
		this.to = to;
		this.date = date;
		this.type = type;
		this.subType = subType;
		this.read = read;
		this.secret = secret;
		this.body = body;
		this.pdu = pdu;
		this.mtype = MessageType.MMS;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public SMSType getType() {
		return type;
	}

	public void setType(SMSType type) {
		this.type = type;
	}

	public SMSSubType getSubType() {
		return subType;
	}

	public void setSubType(SMSSubType subType) {
		this.subType = subType;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public boolean isSecret() {
		return secret;
	}

	public void setSecret(boolean secret) {
		this.secret = secret;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public void setMtype(MessageType mtype) {
		this.mtype = mtype;
	}

	public MessageType getMtype() {
		return mtype;
	}

	public void setPdu(String pdu) {
		this.pdu = pdu;
	}

	public String getPdu() {
		return pdu;
	}

	/**
	 * export an sms record to vsms format (vcard format actually now).
	 * 
	 * @return
	 */
	public String toVSMS() {
		return String.format(vSMSTemplate, 
				SMSProperty.FROM.name(), from,
				SMSProperty.TO.name(), to, 
				SMSProperty.DATE.name(), date.getTime(), 
				SMSProperty.TYPE.name(), type.name(),
				SMSProperty.SUBTYPE.name(), subType.name(), 
				SMSProperty.READ.name(), ((read == true) ? 1 : 0), 
				SMSProperty.BODY.name(), VCardUtils.foldingString(new String(Base64.encodeBase64Chunked(body.getBytes())), VCardUtils.VERSION_VCARD21_INT), 
				SMSProperty.MSG_TYPE.name(), mtype.name(), 
				SMSProperty.PDU.name(),	VCardUtils.foldingString(new String(Base64.encodeBase64Chunked(pdu.getBytes())), VCardUtils.VERSION_VCARD21_INT));
	}

	/**
	 * 
	 * fill each member by parsing the whole bunch of "data" member.
	 * 
	 * TODO: silly implementation now, improve with Android's vcard facility.
	 */
	public void postInit() {
		if ((data == null) || (data.length() == 0)) {
			// TODO: raise exception
			return;
		}

		VCardParser parser = new VCardParser();
		VDataBuilder builder = new VDataBuilder();

		try {
			parser.parse(this.data, builder);
		} catch (VCardException e) {
			return;
		} catch (IOException e) {
			return;
		}

		VNode smsNode = builder.vNodeList.get(0);
		ArrayList<PropertyNode> props = smsNode.propList;

		for (PropertyNode prop : props) {
			if (prop.propName.equalsIgnoreCase(SMSProperty.FROM.name())) {
				this.from = prop.propValue;
			} else if (prop.propName.equalsIgnoreCase(SMSProperty.TO.name())) {
				this.to = prop.propValue;
			} else if (prop.propName.equalsIgnoreCase(SMSProperty.DATE.name())) {
				this.date = new Date(Long.parseLong(prop.propValue));
			} else if (prop.propName.equalsIgnoreCase(SMSProperty.TYPE.name())) {
				this.type = SMSType.valueOf(prop.propValue.toUpperCase());
			} else if (prop.propName.equalsIgnoreCase(SMSProperty.SUBTYPE.name())) {
				this.subType = SMSSubType.valueOf(prop.propValue.toUpperCase());
			} else if (prop.propName.equalsIgnoreCase(SMSProperty.READ.name())) {
				this.read = prop.propValue.equals("1") ? true : false;
			} else if (prop.propName.equalsIgnoreCase(SMSProperty.MSG_TYPE.name())) {
				this.mtype = MessageType.valueOf(prop.propValue.toUpperCase());
			} else if (prop.propName.equalsIgnoreCase(SMSProperty.PDU.name())) {
				/**
				 * NOTE: if the data is encoded in BASE64, VDataBuilder would
				 * store the un-decoded data in propValue, and decoded data (in
				 * bytes) in propValue_bytes.
				 */
				this.pdu = new String(prop.propValue_bytes);
			} else if (prop.propName.equalsIgnoreCase(SMSProperty.BODY.name())) {
				/**
				 * NOTE: if the data is encoded in BASE64, VDataBuilder would
				 * store the un-decoded data in propValue, and decoded data (in
				 * bytes) in propValue_bytes.
				 */
				this.body = new String(prop.propValue_bytes);
			} else {
				// nothing serious
			}
		}
	}

	@Override
	public String toString() {
		return "SMSRecord [body=" + body + ", date=" + date + ", from=" + from
				+ ", read=" + read + ", secret=" + secret + ", subType="
				+ subType + ", to=" + to + ", type=" + type + ", msg_type="
				+ mtype + ", summary = " + pdu + ", data=" + data + ", guid="
				+ guid + ", tag=" + tag + "]";
	}
}

enum SMSProperty {
	FROM, 
	TO, 
	DATE, 
	TYPE, 
	SUBTYPE, 
	READ, 
	SECRET, 
	BODY, 
	MSG_TYPE, 
	PDU
}
