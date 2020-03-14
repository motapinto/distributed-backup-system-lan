package Channels;

import Peer.Peer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Channel {
    private Peer peer;
    private MulticastSocket multicastSocket;

    public enum ChannelType {
        MC, MDB, MDR
    }

    Channel(int port, String address, Peer peer) {
        try {
            this.multicastSocket = new MulticastSocket(port);
            this.multicastSocket.setTimeToLive(1);
            this.multicastSocket.joinGroup(InetAddress.getByName(address));
            this.peer = peer;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
