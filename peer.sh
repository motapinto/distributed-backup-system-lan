function usage {
	echo "Usage: <Number of Peers> <Version>" 
	exit
}

function compile {

	mkdir bin
	javac $(find ./src/* | grep .java) -d bin
	cp -r ./src/TestFiles ./bin
}

function runSnooper {

	xterm -e "java -jar McastSnooper.jar $1:$2 $3:$4 $5:$6" 
	
}

function startRMI {
	
	killall rmiregistry
	xterm -e "rmiregistry -J-Djava.rmi.server.codese=file://$(pwd)/" 
}

function launchPeers {

        count=1
	while [ "$count" -le $1 ]
	do	
    		xterm -e "java Peer.InitPeer $2 $count 200$count $3 $4 $5 $6 $7 $8" & $SHELL &
		count=$(( $count + 1 ))
	done
}

if (( $# != 8 )); then
    usage
fi


MCip=$3
MCport=$4
MDBip=$5
MDBport=$6
MDRip=$7
MDRport=$8
compile
runSnooper $MCip $MCport $MDBip $MDBport $MDRip $MDRport
cd bin
startRMI
launchPeers $1 $2 $MCip $MCport $MDBip $MDBport $MDRip $MDRport
cd .. &