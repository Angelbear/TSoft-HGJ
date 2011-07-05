package cn.edu.tsinghua.hpc.tcontacts.syncaction;

public class SyncState {
	/**
	 * SYNC_STATE_RECOVER: Whether the record is to be recovered
	 * SYNC_STATE_MODIFY: Whether the record is modified SYNC_STATE_DELETE:
	 * Whether the record is to be deleted SYNC_STATE_REMOVE: Whether the record
	 * is to be removed SYNC_STATE_TMP: Whether the record is a tmp record in db
	 * SYNC_STATE_ARCHIVE: Whether the record is to be achived
	 */
	public static final String SYNC_STATE_RECOVER = "recover";
	public static final String SYNC_STATE_UPDATED = "updated";
	public static final String SYNC_STATE_PRESENT = "present";
	public static final String SYNC_STATE_DELETE = "delete";
	public static final String SYNC_STATE_DELETED = "deleted";
	public static final String SYNC_STATE_REMOVE = "remove";
	public static final String SYNC_STATE_TMP = "tmp";

}
