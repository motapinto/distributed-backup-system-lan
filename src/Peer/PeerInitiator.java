package Peer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class PeerInitiator {

    public PeerInitiator(String[] args) {
        String version = args[0];
        String peerId = args[1];
        String serviceAccessPoint = args[2];
        String[] mcAddress = {args[3], args[4]};
        String[] mdbAddress = {args[5], args[6]};
        String[] mdrAddress = {args[7], args[8]};
        // Programmatically set the value of the property java.rmi.server.codebase to the location of the codebase
        System.setProperty("java.rmi.server.codebase", "file:///C://Users/coman/Desktop/sdis1920-t6g06/production/sdis1920-t6g6/RMI/");


        try {
            // Instantiate the "remote object".
            Peer peer = new Peer(version, peerId, serviceAccessPoint, mcAddress, mdbAddress, mdrAddress);

            // Register the stub (returns an object that represents the rmi registry)
            Registry registry;

            if(peerId.equals("1")) {
                registry = LocateRegistry.createRegistry( 1099);
            }
            else{
                registry = LocateRegistry.getRegistry("localhost", 1099);
            }

            // It is preferable to use rebind(…), as bind(…) will throw an exception if the previously registered name is reused
            registry.rebind(serviceAccessPoint, peer);
            System.err.println("Server ready");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        PeerInitiator peer = new PeerInitiator(args);
    }

}
