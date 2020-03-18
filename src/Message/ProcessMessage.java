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
    private String address;
    private int port;

    public ProcessMessage(Peer peer, byte[] message) {
        this.peer = peer;
        this.message = new Message(message);
    }

    public ProcessMessage(Peer peer, byte[] message, int port, String address) {
        this.peer = peer;
        this.message = new Message(message);
        this.port = port;
        this.address = address;
    }

    // Sends message to multicast channel - meter aquilos dos params nas funcoes
    /*public void sendMessage(Message message, String address, int port) {
        DatagramSocket socket;
        DatagramPacket packet;

        try {
            socket = new DatagramSocket();
            packet = new DatagramPacket(message.toString().getBytes(), message.toString().getBytes().length, InetAddress.getByName(this.address), port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }*/


}
