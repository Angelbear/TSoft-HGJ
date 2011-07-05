package cn.edu.tsinghua.hpc.tmms.syncaction;

public class SyncState {
	/**
	 *  SYNC_STATE_RECOVER:  Whether the record is to be recovered
	 *  SYNC_STATE_MODIFY: Whether the record is modified
	 *  SYNC_STATE_DELETE: Whether the record is to be deleted
	 *  SYNC_STATE_REMOVE: Whether the record is to be removed
	 *  SYNC_STATE_TMP: Whether the record is a tmp record in db
	 *  SYNC_STATE_ARCHIVE: Whether the record is to be achived
	 */
	//是不是每步操作需要两个状态，recovering，recovered，防止对一条短信多次同步
	public static final String SYNC_STATE_RECOVER      = "recover";//从回收站回复,未同步到云端
	public static final String SYNC_STATE_UPDATED      = "updated";
	public static final String SYNC_STATE_PRESENT      = "present";//新插入未做任何操作/从回收站恢复并已同步到云端
	public static final String SYNC_STATE_DELETED      = "deleted";//进入回收站，未同步到云端
	public static final String SYNC_STATE_REMOVED      = "removed";//从本地和云端彻底删除
	public static final String SYNC_STATE_TMP     	   = "tmp";
	public static final String SYNC_STATE_NOT_SYNC	   = "notsync";//未同步到云端
	public static final String SYNC_STATE_ARCHIVED	   = "archived";
	public static final String SYNC_STATE_REMOTE_DELETE = "remotedeleted";//进入回收站，已经同步到云端
	
	public static final int SYNC_DISABLED = 0;
	public static final int SYNC_ENABLED = 1;
	public static final int SYNC_DISABLE = 2;
	public static final int SYNC_ENABLE = 3;
}
