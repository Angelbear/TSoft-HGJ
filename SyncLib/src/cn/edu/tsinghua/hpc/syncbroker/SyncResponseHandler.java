package cn.edu.tsinghua.hpc.syncbroker;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SyncResponseHandler {
	private static DocumentBuilder db;
	private static Document doc;

	public SyncResponseHandler() {
		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setXMLData(String ack) {
		try {
			doc = db.parse(new InputSource(new StringReader(ack)));
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	private String getSingleTagText(String tag) {
		NodeList items = doc.getElementsByTagName(tag);
		if (items.getLength() > 0) {
			return items.item(0).getFirstChild().getNodeValue();
		}
		
		return null;
	}

	private int getIntegerByTag(String tag) {
		int i = -1;
		
		try {
			i = Integer.parseInt(getSingleTagText(tag));
		} catch (Exception e) {
			i = -1;
		}
		
		return i;
	}
	
	public int handleAddACK(String ack) {
		if (ack.length() == 0) {
			return -1;
		}
		
		setXMLData(ack);	
		return getIntegerByTag(XMLTag.GUID.name());
	}
	
	public boolean handleUpdateACK(String ack) {
		if (ack.length() == 0) {
			return false;
		}
		
		return true;
	}
	
	public boolean handleMarkACK(String ack) {
		if (ack.length() == 0) {
			return false;
		}
		
		return true;
	}
	
	public List<SyncRecord> handleSearchACK(String ack) {
		List<SyncRecord> result = new ArrayList<SyncRecord>();
	
		if (ack.length() == 0) {
			return result;
		}
	
		setXMLData(ack);
		int cn = getIntegerByTag(XMLTag.ItemNumber.name());
		if (cn > 0) {
			/**
			 * <ItemNumber>2</ItemNumber>
			 * <ItemList type="contact|sms">
			 *   <Item tag="CACHED">
			 *     <GUID>xxx</GUID>
			 *   </Item>
			 * <Item tag="ARCHIVED">
			 *     <GUID>xxx</GUID>
			 *     <DATA>vCard or vSMS data</DATA>
			 * </Item>
			 * </ItemList>
			 * */
			// parse ItemList to get record type
			Node item = doc.getElementsByTagName(XMLTag.ItemList.name()).item(0); // only one ItemList node
			Attr tagAttr = (Attr)item.getAttributes().item(0); // only one "type=xx" attribute
			SyncRecordType itemType = SyncRecordType.valueOf(tagAttr.getValue());
			
			// parse each Item
			NodeList items = doc.getElementsByTagName(XMLTag.Item.name());
			for (int i = 0; i < cn; i++) {
				item = items.item(i);

				tagAttr = (Attr)item.getAttributes().item(0);
				SyncTag tag = SyncTag.mapTag(tagAttr.getValue());

				NodeList properties = item.getChildNodes();
				int guid = -1;
				String data = "";
				for (int j = 0; j < properties.getLength(); j++) {		
					Node property = properties.item(j);
					if (property.getNodeName().equalsIgnoreCase(XMLTag.GUID.name())) {
						guid = Integer.parseInt(property.getFirstChild().getNodeValue());
					}
					if (property.getNodeName().equalsIgnoreCase(XMLTag.DATA.name())) {
						data = property.getFirstChild().getNodeValue();
					}							
				}
				result.add(SyncRecordFactory.createSyncRecord(itemType, guid, tag, data));				
			}
		}
		return result;
	}
	
	public boolean handleRemoveACK(String ack) {
		if (ack.length() == 0) {
			return false;
		}
		
		return true;
	}
	
	public int handleGetCountACK(String ack) {
	
		if (ack.length() == 0) {
			return -1;
		}
	
		setXMLData(ack);
		/**
		 * <ItemCount>2</ItemCount>
		 * */
		return getIntegerByTag(XMLTag.ItemCount.name());

	}	
}

