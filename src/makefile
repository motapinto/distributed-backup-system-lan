
JFLAGS = -g
JC = javac

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
        Channels/Channel.java \
		Channels/MC.java \
		Channels/MDB.java \
		Channels/MDR.java \
		Common/Constants.java \
		Common/Logs.java \
		Common/Utilities.java \
		Message/Dispatcher.java \
		Message/Header.java \
		Message/Message.java \
		Peer/Peer.java \
		Peer/PeerInterface.java \
		SubProtocols/Backup.java \
        SubProtocols/Delete.java \
        SubProtocols/Restore.java \
        SubProtocols/SpaceReclaim.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class