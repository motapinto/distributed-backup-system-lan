package SubProtocols;

import Common.Utilities;
import Message.Message;
import Peer.Peer;
import Message.Dispatcher;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static Common.Constants.DELETE;

public class Delete {

    private Peer peer;
    private String pathName;
    private String fileId;

    /**
     * Responsible for deleting a file
     *
     * @param peer          : peer listening to the multicast
     * @param pathname      : pathname to the file
     */
    public Delete(Peer peer, String pathname) {
        this.pathName = pathname;
        this.peer = peer;
    }

    /**
     * Creates delete protocol
     *
     * @param peer : peer that creates delete protocol
     */
    public Delete(Peer peer) {
        this.peer = peer;
    }

    public void startDeleteProcedure() {
        this.setFileId();
        this.sendDeleteMessage();
        this.deleteFile(this.peer, this.fileId);
    }

    public void setFileId() {
        File file = new File(this.pathName);
        this.fileId = Utilities.hashAndEncode(file.getName() + file.lastModified() + file.length());
    }

    /**
     * Sends a DELETE
     */
    public void sendDeleteMessage() {
        Message request = new Message(DELETE, this.peer.getVersion(), Integer.toString(this.peer.getId()), this.fileId);

        for(int i = 0; i < 5; i++) {
            Dispatcher dispatcher = new Dispatcher(this.peer, request, this.peer.getControlChannel());
            this.peer.getSenderExecutor().submit(dispatcher);
        }
    }

    /**
     * Deletes all chunks from a file if original copy or removes entirely the original copy of the file
     * depending on the received peer
     *
     *
     * String : "fileId_chuckNo"   |   String : "repDegree_desiredRepDegree"
     */
    public void deleteFile(Peer peer, String fileId) {


        if(this.peer.equals(peer)) {

        } else {


        }



        Map<String, String> repDegreeInfo = this.peer.getRepDegreeInfo();


    }




}
