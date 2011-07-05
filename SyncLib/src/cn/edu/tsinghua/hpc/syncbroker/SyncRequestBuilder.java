package cn.edu.tsinghua.hpc.syncbroker;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Format the request XML.
 * 
 * TODO: SIM Serial number is used as the uesrid.
 * 
 * @author xrn
 * 
 */
public class SyncRequestBuilder {
	// final private String phone;
	final private String userID;

	public SyncRequestBuilder(Context context) {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		// phone = tm.getLine1Number();
		// userID = tm.getSimSerialNumber();
		userID = tm.getSubscriberId();
	}
	
	public SyncRequestBuilder(Context context, String user) {
		// TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		// phone = tm.getLine1Number();
		// userID = tm.getSimSerialNumber();
		// userID = tm.getSubscriberId();
		if (user == null) {
			TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			userID = tm.getSubscriberId();
		} else {
			userID = user;
		}
	}

	private static String xmlRequestTemplate = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
			"<%s>\n" + // Request tag
			"  <%s>%s</%s>\n" + // userID
			"  <%s>%s</%s>\n" + // cmd
			"  %s" + // main body
			"</%s>"; // Request tag

	public String buildXML(SyncCommand csc, HashMap<String, String> data)
			throws ElementNotFound {
		if ((data == null) || data.isEmpty()) {
			return null;
		}
		String xml = String.format(xmlRequestTemplate, 
			     XMLTag.Request.name(),
			     XMLTag.USERID.name(), userID, XMLTag.USERID.name(), 
			     XMLTag.CMD.name(), SyncCommand.getSyncCommandOut(csc).name(), XMLTag.CMD.name(), 
			     buildCommandBody(csc, data), 
			     XMLTag.Request.name());
		return xml;
	}

	private String buildField(String key, HashMap<String, String> data)
			throws ElementNotFound {
		if (data == null || !data.containsKey(key)) {
			throw new ElementNotFound("Not enough data provided. Or it does not have the key: " + key);
		}

		return String.format("<%s>%s</%s>\n", key, escapeXML(data.get(key)), key);
	}

	private String buildCommandBody(SyncCommand csc,
					HashMap<String, String> data) throws ElementNotFound {
		StringBuilder body = new StringBuilder();

		SyncCommandOut csco = SyncCommand.getSyncCommandOut(csc);

		// insert <TAG> value into data if it is MARK, because the user is
		// unaware of MARK.
		if (csco == SyncCommandOut.MARK) {
			data.put(XMLTag.TAG.name(), SyncCommand.getSyncCammandTag(csc).name());
		}

		for (XMLTag tag : SyncCommandTagList.getXMLTagList(csco)) {
			body.append(buildField(tag.name(), data));
		}

		return body.toString();
	}

	/**
	 * Escape characters for text appearing as XML data, between tags.
	 * 
	 * <P>
	 * The following characters are replaced with corresponding character
	 * entities :
	 * <table border='1' cellpadding='3' cellspacing='0'>
	 * <tr><th>Character</th><th>Encoding</th></tr>
	 * <tr><td><</td><td>&lt;</td></tr>
	 * <tr><td>></td><td>&gt;</td></tr>
	 * <tr><td>&</td><td>&amp;</td></tr>
	 * <tr><td>"</td><td>&quot;</td></tr>
	 * <tr><td>'</td><td>&#039;</td></tr>
	 * </table>
	 * 
	 * <P>
	 * Note that JSTL's {@code <c:out>} escapes the exact same set of characters
	 * as this method. <span class='highlight'>That is, {@code <c:out>} is good
	 * for escaping to produce valid XML, but not for producing safe
	 * HTML.</span>
	 */
	public static String escapeXML(String aText) {
		final StringBuilder result = new StringBuilder();
		final StringCharacterIterator iterator = new StringCharacterIterator(aText);
		char character = iterator.current();
		while (character != CharacterIterator.DONE) {
			if (character == '<') {
				result.append("&lt;");
			} else if (character == '>') {
				result.append("&gt;");
			} else if (character == '\"') {
				result.append("&quot;");
			} else if (character == '\'') {
				result.append("&#039;");
			} else if (character == '&') {
				result.append("&amp;");
			} else {
				// the char is not a special one
				// add it to the result as is
				result.append(character);
			}
			character = iterator.next();
		}
		return result.toString();
	}

}

/**
 * 
 * Map each command to the required tags.
 * 
 * */
final class SyncCommandTagList {
	private static HashMap<SyncCommandOut, XMLTag[]> cmdTagTable;

	static {
		cmdTagTable = new HashMap<SyncCommandOut, XMLTag[]>();
		cmdTagTable.put(SyncCommandOut.ADD, new XMLTag[] { XMLTag.DATA });
		cmdTagTable.put(SyncCommandOut.UPDATE, new XMLTag[] { XMLTag.GUID,
				XMLTag.DATA });
		cmdTagTable.put(SyncCommandOut.MARK, new XMLTag[] { XMLTag.TAG,
				XMLTag.FILTER });
		cmdTagTable.put(SyncCommandOut.SEARCH, new XMLTag[] { XMLTag.FILTER });
		cmdTagTable.put(SyncCommandOut.FIRSTSYNC,
				new XMLTag[] { XMLTag.FILTER });
		cmdTagTable.put(SyncCommandOut.REMOVE, new XMLTag[] { XMLTag.FILTER });
		cmdTagTable.put(SyncCommandOut.GETCOUNT, new XMLTag[] { XMLTag.FILTER });
	}

	public static XMLTag[] getXMLTagList(SyncCommandOut csco) {
		return cmdTagTable.get(csco);
	}
}
