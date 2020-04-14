import Peer.PeerInterface;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {

    private final String peerAccessPoint;
    private final String protocol;
    private String operand1;
    private String operand2;
    private PeerInterface peer;

    public TestApp(String peerAccessPoint, String protocol, String operand1, String operand2) {
        this.peerAccessPoint = peerAccessPoint;
        this.protocol = protocol;
        this.operand1 = operand1;
        this.operand2 = operand2;
    }

    public TestApp(String peerAccessPoint, String protocol, String operand1) {
        this.peerAccessPoint = peerAccessPoint;
        this.protocol = protocol;
        this.operand1 = operand1;
        this.operand2 = null;
    }

    public TestApp(String peerAccessPoint, String protocol) {
        this.peerAccessPoint = peerAccessPoint;
        this.protocol = protocol;
    }

    public void init() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            PeerInterface peer = (PeerInterface) registry.lookup(this.peerAccessPoint);
            this.peer = peer;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void test() throws RemoteException {
        switch (this.protocol) {
            case "BACKUP":
                this.peer.backup(this.operand1, Integer.parseInt(this.operand2));
                break;
            case "RESTORE":
                this.peer.restore(this.operand1);
                break;
            case "DELETE":
                this.peer.delete(this.operand1);
                break;
            case "RECLAIM":
                this.peer.reclaim(Integer.parseInt(this.operand1));
                break;
            case "STATE":
                this.peer.state();
                break;
        }
    }

    public static void main(String[] args) throws RemoteException {
        String peerAccessPoint = args[0];
        String protocol = args[1];
        TestApp application;

        if(protocol.equals("STATE")) {

            application = new TestApp(peerAccessPoint, protocol);
        } else if(protocol.equals("BACKUP")) {
            String operand1 = args[2];
            String operand2 = args[3];
            application = new TestApp(peerAccessPoint, protocol, operand1, operand2);
        } else {
            String operand1 = args[2];
            application = new TestApp(peerAccessPoint, protocol, operand1);
        }

        application.init();
        application.test();
    }
}