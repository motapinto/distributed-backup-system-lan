package Channels;

import Common.Logs;
import Peer.Peer;
import java.io.IOException;

public class MDB extends Channel {

    /**
     * Class responsible for the backup channel
     *
     * @param peer : peer listening to the multicast
     * @param address : multicast address
     * @param port : multicast port
     */
    public MDB(Peer peer, String address, int port) {
        super(peer, address, port);
        Logs.log("Backup channel initialized");
    }
}
