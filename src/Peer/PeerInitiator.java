package Peer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class PeerInitiator {

    public PeerInitiator(String[] args) {

        String version = args[0];
        String peerId = args[1];
        String serviceAccessPoint = args[2];
        String[] mcAddress = {args[3], args[4]};
        String[] mdbAddress = {args[5], args[6]};
        String[] mdrAddress = {args[7], args[8]};

        Peer peer = new Peer(version, peerId, serviceAccessPoint, mcAddress, mdbAddress, mdrAddress);
        Registry registry = null;
        try {
            registry = LocateRegistry.getRegistry();
            registry.rebind(serviceAccessPoint, peer);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        PeerInitiator peer = new PeerInitiator(args);
    }

}
