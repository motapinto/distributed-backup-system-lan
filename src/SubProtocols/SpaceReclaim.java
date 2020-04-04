package SubProtocols;

import Peer.Peer;

public class SpaceReclaim {

    private Peer peer;
    private int sizeToReclaim;

    /**
     * Creates restore protocol
     *
     * @param peer : peer that creates delete protocol
     */
    public SpaceReclaim(Peer peer) {
        this.peer = peer;
        this.sizeToReclaim = 0;
    }

    /**
     * Responsible for reclaiming a file
     *
     * @param peer          : peer listening to the multicast
     * @param sizeToReclaim : size that peer intends to reclaim
     */
    public SpaceReclaim(Peer peer, int sizeToReclaim) {
        this.peer = peer;
        this.sizeToReclaim = sizeToReclaim;
    }


}


