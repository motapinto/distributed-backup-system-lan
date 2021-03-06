package Peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerInterface extends Remote{

    /**
     * Function used by the user to back-up a determined file
     *
     * @param pathname          : name of the file to backup - should be in the peer's files directories
     * @param replicationDegree : desired replication degree for the file
     */
    void backup(String pathname, int replicationDegree) throws RemoteException;;

    /**
     * Function used by the user to restore a determined file
     *
     * @param pathname name of the file to restore - should be in the peer's files directories
     */
    void restore(String pathname) throws RemoteException;;

    /**
     * Function used by the user to delete a determined file
     *
     * @param pathname name of the file to delete - should be in the peer's files directories
     */
    void delete(String pathname) throws RemoteException;;

    /**
     * Function used by the user to tell to a determined peer the maximum disk space used for
     * storing chunks
     *
     * @param maxDiskSpace
     */
    void reclaim(int maxDiskSpace) throws RemoteException;;

    /**
     * Prints the state.............................
     */
    void state() throws RemoteException;
}
