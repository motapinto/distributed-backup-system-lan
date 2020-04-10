package Peer;

import java.io.IOException;
import java.rmi.Remote;

public interface PeerInterface extends Remote {

    /**
     * Function used by the user to back-up a determined file
     *
     * @param pathname          : name of the file to backup - should be in the peer's files directories
     * @param replicationDegree : desired replication degree for the file
     */
    void backup(String pathname, int replicationDegree);

    /**
     * Function used by the user to restore a determined file
     *
     * @param pathname name of the file to restore - should be in the peer's files directories
     */
    void restore(String pathname);

    /**
     * Function used by the user to delete a determined file
     *
     * @param pathname name of the file to delete - should be in the peer's files directories
     * @throws IOException
     */
    void delete(String pathname);

    /**
     * Function used by the user to tell to a determined peer the maximum disk space used for
     * storing chunks
     *
     * @param maxDiskSpace
     */
    void reclaim(int maxDiskSpace);

    /**
     * Prints the state.............................
     */
    void state();
}
