package Channels;

import Common.Logs;
import Peer.Peer;

public class MC extends Channel {

    /**
     * Class responsible for the control channel
     *
     * @param peer : peer listening to the multicast
     * @param address : multicast address
     * @param port : multicast port
     */
    public MC(Peer peer, String address, int port) {
        super(peer, address, port);
        Logs.log("Control channel initialized");
    }
}