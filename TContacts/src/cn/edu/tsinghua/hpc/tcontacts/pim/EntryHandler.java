package cn.edu.tsinghua.hpc.tcontacts.pim;


public interface EntryHandler {
    /**
     * Called when the parsing started.
     */
    public void onParsingStart();

    /**
     * The method called when one VCard entry is successfully created
     */
    public void onEntryCreated(final ContactStruct entry);

    /**
     * Called when the parsing ended.
     * Able to be use this method for showing performance log, etc.
     */
    public void onParsingEnd();
}