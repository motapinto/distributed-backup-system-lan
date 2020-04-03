package SubProtocols;

import Peer.Peer;

public class SpaceReclaim {

    private Peer peer;
    private String pathName;

    /**
     * Responsible for reclaiming a file
     *
     * @param peer          : peer listening to the multicast
     * @param pathname      : pathname to the file
     */
    public SpaceReclaim(Peer peer, String pathname) {
        this.peer = peer;
        this.pathName = pathname;
    }

    /**
     * Creates restore protocol
     *
     * @param peer : peer that creates delete protocol
     */
    public SpaceReclaim(Peer peer) {
        this.peer = peer;
    }


}


