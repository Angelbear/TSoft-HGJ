package cn.edu.tsinghua.hpc.syncbroker;

public final class SyncRecordFactory {
	public static SyncRecord createSyncRecord(SyncRecordType type, int guid, SyncTag tag, String data) {
		switch (type) {
		case CONTACT:
			return new ContactRecord(guid, tag, data);
		case SMS:
			return new SMSRecord(guid, tag, data);
		default:
			return null;
		}
	}
}
