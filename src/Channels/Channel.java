package Channels;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import Common.Logs;
import Message.Dispatcher;
import Peer.Peer;
import static Common.Constants.MAX_PACKET_SIZE;

public class Channel implements Runnable {

    protected final Peer peer;
    protected final int port;
    protected final String address;
    protected MulticastSocket multicastSocket;

    /**
     * Class responsible with the communication with the multicast
     *
     * @param peer : peer listening to the multicast
     * @param address : multicast address
     * @param port : multicast port
     */
    Channel(Peer peer, String address, int port) {
        this.peer = peer;
        this.address = address;
        this.port = port;
        try {
            this.multicastSocket = new MulticastSocket(this.port);
            this.multicastSocket.setTimeToLive(1);
            InetAddress ipAddress = InetAddress.getByName(address);
            this.multicastSocket.joinGroup(ipAddress);
        } catch (IOException e) {
            Logs.logError("Error while creating a multicast socket");
            e.printStackTrace();
        }
    }

    /**
     * Receives packets from the multicast socket
     *
     * @return DatagramPacket : received packet
     * @throws IOException
     */
    public DatagramPacket receive() throws IOException {
        byte[] buf = new byte[MAX_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        this.multicastSocket.receive(packet);
        return packet;
    }

    /**
     * Runs the Channel thread that handles received packets
     */
    @Override
    public void run() {
        while (true) {
            try {
                DatagramPacket received = this.receive();
                Dispatcher handler = new Dispatcher(this.peer, received);
                this.peer.getReceiverExecutor().submit(handler);
            } catch (IOException e) {
                Logs.logError("Error handling peer" + e);
            }
        }
    }

    public Peer getPeer() {
        return this.peer;
    }

    public int getPort() { return port; }

    public String getAddress() { return address; }
}