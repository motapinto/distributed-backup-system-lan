package Peer;

import Channels.*;



public class Peer {
    private String version;
    private int id;
    private String[] serviceAccessPoint;

    public Peer(String version, String id, String[] serviceAccessPoint, String[] mcAddress, String[] mdbAddress, String[] mdrAddresss) {
        this.version = version;
        this.id = Integer.parseInt(id);
        this.serviceAccessPoint = serviceAccessPoint;

        MC controlChannel = new MC(this, mcAddress[0], mcAddress[1]);
        MDB backupChannel = new MDB(this, mdbAddress[0], mdbAddress[1]);
        MDR restoreChannel = new MDR(this, mdrAddresss[0], mdrAddresss[1]);





    }
}
