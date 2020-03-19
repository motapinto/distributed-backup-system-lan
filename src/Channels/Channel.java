package Channels;

import Common.Logs;
import Message.Dispatcher;
import Message.Message;
import Peer.Peer;
import static Common.Constants.MAX_PACKET_SIZE;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;


public class Channel implements Runnable {
    protected Peer peer;
    protected int port;
    protected InetAddress address;
    protected MulticastSocket multicastSocket;
    protected ScheduledExecutorService threadDispatcher;

    /**
     * Class responsible for the comunication with the multicast
     *
     * @param peer : peer listening to the multicast
     * @param address : multicast address
     * @param port : multicast port
     * @throws IOException
     */
    Channel(Peer peer, String address, int port) throws IOException {
        this.peer = peer;
        this.address = InetAddress.getByName(address);
        this.port = port;
        this.multicastSocket = new MulticastSocket(this.port);
        this.multicastSocket.setTimeToLive(1);
        this.multicastSocket.joinGroup(this.address);
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
                Message messageReceived = new Message(this.receive());
                if(!messageReceived.getHeader().getSenderId().equals(this.peer.getId())) {
                    ScheduledThreadPoolExecutor repetitiveTask = new ScheduledThreadPoolExecutor(1);
                    Dispatcher handler = new Dispatcher(this.peer, messageReceived);
                    handler.start();
                }
            } catch (IOException e) {
                Logs.logError("Error handling peer" + e);
            }
        }
    }

    /**
     * Gets peer connected to the channel
     *
     * @return Peer peer : peer connected to the chanel
     */
    public Peer getPeer() {
        return this.peer;
    }

    /**
     * Closes the multicast socket
     */
    public void closeSocket() {
        this.multicastSocket.close();
    }
}