package SubProtocols;

import Common.Utilities;
import Message.Message;
import Peer.Peer;
import Message.Dispatcher;

import java.io.File;
import java.util.Map;
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

        Map<String, String> repDegreeInfo = peer.getRepDegreeInfo();
        Map<String, String> storedHistory = peer.getStoredChunkHistory();

        if(this.peer.equals(peer)) {
            File file =new File(this.peer.FILE_STORAGE_PATH + "/" + fileId);
            int bytes = (int)file.length();

            for(Map.Entry<String, String> entry : repDegreeInfo.entrySet()) {
                String key = entry.getKey();
                if(key.split("_")[0].equals(fileId)) {
                    //estamos a mudar o mapa efetivamente assim? ou temos de mudar no peer?
                    repDegreeInfo.remove(key);
                    storedHistory.remove(key);
                    this.peer.setCurrentSystemMemory(this.peer.getCurrentSystemMemory() - bytes);
                }

            }

        } else {


        }


    }




}
