package cn.edu.tsinghua.hpc.syncbroker;

/**
 * Status tag for each contact in the db.
 * */
enum SyncTag {
	INVALID,
	CACHED,
	ARCHIVED,
	DELETED;
	
	public static SyncTag mapTag(String tag) {
		for (SyncTag t: SyncTag.values()) {
			if (tag.equalsIgnoreCase(t.name())) {
				return t;
			}
		}
		return INVALID;
	}
}