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
	//�ǲ���ÿ��������Ҫ����״̬��recovering��recovered����ֹ��һ�����Ŷ��ͬ��
	public static final String SYNC_STATE_RECOVER      = "recover";//�ӻ���վ�ظ�,δͬ�����ƶ�
	public static final String SYNC_STATE_UPDATED      = "updated";
	public static final String SYNC_STATE_PRESENT      = "present";//�²���δ���κβ���/�ӻ���վ�ָ�����ͬ�����ƶ�
	public static final String SYNC_STATE_DELETED      = "deleted";//�������վ��δͬ�����ƶ�
	public static final String SYNC_STATE_REMOVED      = "removed";//�ӱ��غ��ƶ˳���ɾ��
	public static final String SYNC_STATE_TMP     	   = "tmp";
	public static final String SYNC_STATE_NOT_SYNC	   = "notsync";//δͬ�����ƶ�
	public static final String SYNC_STATE_ARCHIVED	   = "archived";
	public static final String SYNC_STATE_REMOTE_DELETE = "remotedeleted";//�������վ���Ѿ�ͬ�����ƶ�
	
	public static final int SYNC_DISABLED = 0;
	public static final int SYNC_ENABLED = 1;
	public static final int SYNC_DISABLE = 2;
	public static final int SYNC_ENABLE = 3;
}
