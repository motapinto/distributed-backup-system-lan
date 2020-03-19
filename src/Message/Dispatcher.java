package Message;

import Peer.Peer;
import Channels.Channel;
import static Common.Constants.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Dispatcher implements Runnable {
    private Peer peer;
    private Message message;

    /**
     * Receives a datagram packet from the a channel and handles it
     *
     * @param peer : peer that receives packet
     * @param packet : packet received from the channel
     */
    public Dispatcher(Peer peer, DatagramPacket packet) {
        this.peer = peer;
        this.message = new Message(packet);
    }

    /**
     * Receives a message to handle and dispatch
     *
     * @param peer : peer that receives packet
     * @param message : packet received from the channel
     */
    public Dispatcher(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void run() {
        switch (this.message.getHeader().getMessageType()) {
            case PUTCHUNK:
                this.peer.backup();
                break;
            case GETCHUNK:
                break;
            case CHUNK:
                break;
            case DELETE:
                break;
            case REMOVED:
                break;
            case STORED:
                break;
        }
    }
}
