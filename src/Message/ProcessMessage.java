package Message;

import Peer.Peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

// vai chamar os protocolos de acordos com os pedidos

public class ProcessMessage {
    private Peer peer;
    private Message message;

    public ProcessMessage(Peer peer, byte[] message) {
        this.peer = peer;
        this.message = new Message(message);
    }

    public ProcessMessage(Peer peer, byte[] message, Channel channel) {
        this.peer = peer;
        this.message = new Message(message);
    }

    public void dispatchMessage(){









    }












}
