import Peer.PeerInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {

    private final String peerAccessPoint;
    private final String protocol;
    private final String operand1;
    private final String operand2;
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

    public void init() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 2020);
            PeerInterface peer = (PeerInterface) registry.lookup(this.peerAccessPoint);
            this.peer = peer;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void test() {
        switch (this.protocol) {
            case "BACKUP":
                assert this.operand2 != null;
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

    public static void main(String[] args) {
        String peerAccessPoint = args[0];
        String protocol = args[1];
        String operand1 = args[2];
        String operand2;
        TestApp application;

        if(protocol.equals("BACKUP")) {
            operand2 = args[3];
            application = new TestApp(peerAccessPoint, protocol, operand1, operand2);
        } else {
            application = new TestApp(peerAccessPoint, protocol, operand1);
        }

        application.init();
        application.test();
    }
}
