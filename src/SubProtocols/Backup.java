package SubProtocols;

public class Backup {
    /* private String fileId;
    private int replicationDeg;
    private int chunkNumber;
    private bytes[] body;
    private String senderId;*/
    private Peer parentPeer;
    private Message msg;

    public Backup(Peer peer, Message msg){

        this.parentPeer = peer;
        this.msg = msg;
    }

    public boolean putChunk(Message msg){

        /* Não sei se temos que verificar que o senderID aqui ou se verifcamos quando recebemos a messagem no canal */
        /* verificar se o chunk já está stored no peer*/
        /* caso não esteja verificar se existe espaço suficiiente para guardar o ficheiro*/
        /* caso tenha espaço, vamos ver se já existe o diretorio, senão criar diretorio do tipo FILEID/CHUNKNO.txt*/
        /* store do chunk no diretório */
        /* em caso de sucesso criar a mensagem STORED para enviar para o canal MC para o peer que envio o PUTCHUNK*/

        if(!getChunk(msg) && this.peer.getAvailableStorage() > msg.getBody().getBytes().length){

        }
        
    }

    public boolean getChunk(){

        /*Message message = new Message();*/


    }

}
