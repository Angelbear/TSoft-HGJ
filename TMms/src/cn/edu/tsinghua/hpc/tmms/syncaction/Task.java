package cn.edu.tsinghua.hpc.tmms.syncaction;

public class Task {
	private String cmd;
	private String localId;
	private boolean isIDLocal = true;//available before inserting into db
	private String filter;

	public Task(String cmd, String id, boolean IDLocal) {
		this.cmd = cmd;
		this.localId = id;
		this.isIDLocal = IDLocal;
	}
	
	public Task(String cmd,boolean isLocal,String filter) {
		this.cmd = cmd;
		this.filter =  filter;
		this.localId = null;
		this.isIDLocal = false;
	}

	public Task(String cmd, String id) {
		this.cmd = cmd;
		this.localId = id;
		this.isIDLocal = true;
	}

	public String getCmd() {
		return cmd;
	}

	public String getLocalId() {
		return localId;
	}

	public boolean isIDLocal() {
		return isIDLocal;
	}
}
