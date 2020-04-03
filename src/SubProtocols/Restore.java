package SubProtocols;

import Peer.Peer;

public class Restore {

    private Peer peer;
    private String pathName;

    /**
     * Responsible for restoring a file
     *
     * @param peer          : peer listening to the multicast
     * @param pathname      : pathname to the file
     */
    public Restore(Peer peer, String pathname) {
        this.peer = peer;
        this.pathName = pathname;
    }

    /**
     * Creates restore protocol
     *
     * @param peer : peer that creates delete protocol
     */
    public Restore(Peer peer) {
        this.peer = peer;
    }


}
