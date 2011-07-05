package cn.edu.tsinghua.hpc.syncbroker;

/**
 * User sync commands.
 * */
public enum SyncCommand {
	ADD,
	UPDATE,
	
	DEACTIVATE,	// MARK ARCHIVED
	ACTIVATE,	// MARK CACHED
	RECYCLE,	// MARK DELETED
	RECOVER,	// MARK CACHED
	
	FIRSTSYNC,  // sync for a new phone
	SEARCH,
	REMOVE,
	GETCOUNT;
	
	protected static final SyncCommandOut getSyncCommandOut(SyncCommand csc) {
		switch (csc) {
			case ADD:
				return SyncCommandOut.ADD;
			case UPDATE:
				return SyncCommandOut.UPDATE;
			case FIRSTSYNC:
				return SyncCommandOut.FIRSTSYNC;
			case SEARCH:
				return SyncCommandOut.SEARCH;
			case REMOVE:
				return SyncCommandOut.REMOVE;
			case GETCOUNT:
				return SyncCommandOut.GETCOUNT;				
			default:
				return SyncCommandOut.MARK; 
		}
	}
	
	protected static final SyncTag getSyncCammandTag(SyncCommand csc) {
		if (getSyncCommandOut(csc) != SyncCommandOut.MARK) {
			return SyncTag.INVALID;
		}
		
		switch (csc) {
			case DEACTIVATE:
				return SyncTag.ARCHIVED;
			case ACTIVATE:
				return SyncTag.CACHED;
			case RECYCLE:
				return SyncTag.DELETED;
			case RECOVER:
				return SyncTag.CACHED;
			default:
				return SyncTag.INVALID;
		}
	}
}

/**
 * Commands send out to server.
 * */
enum SyncCommandOut {
	FIRSTSYNC,
	ADD,
	UPDATE,	
	MARK,	
	SEARCH,
	REMOVE,
	GETCOUNT
}