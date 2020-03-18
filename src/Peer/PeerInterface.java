package Peer;

public interface PeerInterface  {


    /**
     * Function used by the user to back-up a determined file
     *
     * @param pathname          name of the file to backup - should be in the peer's files directories
     * @param replicationDegree desired replication degree for the file
     * @throws RemoteException
     */
    void backup(String pathname, int replicationDegree) throws IOException;

    /**
     * Function used by the user to restore a determined file
     *
     * @param pathname name of the file to restore - should be in the peer's files directories
     * @throws RemoteException
     */
    void restore(String pathname) throws IOException;

    /**
     * Function used by the user to delete a determined file
     *
     * @param pathname name of the file to delete - should be in the peer's files directories
     * @throws RemoteException
     */
    void delete(String pathname) throws IOException;

    /**
     * Function used by the user to tell to a determined peer the maximum disk space used for
     * storing chunks
     *
     * @param maxDiskSpace
     * @throws RemoteException
     */
    void reclaim(int maxDiskSpace) throws RemoteException;




}
