package cn.edu.tsinghua.hpc.syncbroker;

public class SyncRecord {
	public static final int ID_IN_MEMORY = -2;
	public int _id = ID_IN_MEMORY;
	protected int guid;
	protected SyncTag tag;
	protected String data;
	
	public int getGuid() {
		return guid;
	}

	public void setGuid(int guid) {
		this.guid = guid;
	}

	public SyncTag getTag() {
		return tag;
	}

	public void setTag(SyncTag tag) {
		this.tag = tag;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getData() {
		return data;
	}
	
	public SyncRecord() {
		this.guid = -1;
		this.tag = SyncTag.INVALID;
		this.data = null;
	}
	
	public SyncRecord(int guid, SyncTag tag, String data) {
		this.guid = guid;
		this.tag = tag;
		this.data = data;
	}

	@Override
	public String toString() {
		return "SyncRecord [data=" + data + ", guid=" + guid + ", tag=" + tag
				+ "]";
	}
}

enum SyncRecordType {
	CONTACT,
	SMS
}
