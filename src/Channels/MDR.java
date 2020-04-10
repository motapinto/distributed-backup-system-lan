package Channels;

import Common.Logs;
import Peer.Peer;
import java.io.IOException;

public class MDR extends Channel{

    /**
     * Class responsible for the restore channel
     *
     * @param peer : peer listening to the multicast
     * @param address : multicast address
     * @param port : multicast port
     */
    public MDR(Peer peer, String address, int port) {
        super(peer, address, port);
        Logs.log("Restore channel initialized");
    }
}
