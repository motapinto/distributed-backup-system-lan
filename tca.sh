#!/bin/bash

function launchTCA {

	xterm -e "java TestingClientApplication.TCA "$1" "$2" "$3" "$4"" & $SHELL &
}

function usage {

	case $1 in
		BACKUP )
			echo "Usage: <Access Point> <Protocol> <File> <Replication Degree>"
			exit ;;
		RESTORE )
			echo "Usage: <Access Point> <Protocol> <File>"
			exit ;;
		SPACERECLAIM )
			echo "Usage: <Access Point> <Protocol> <Number of Bytes>"
			exit ;;
		STATE )
			echo "Usage: <Access Point> <Protocol>"
			exit ;;
		DELETE )
			echo "Usage: <Access Point> <Protocol> <File>"
			exit ;;
		EMPTY )
			echo "Usage: <Access Point> <Protocol> [ <Number of Bytes> | <File> | <File>, <Replication Degree>]"
			exit ;;
	esac
}

cd bin

if (( $# == 0 )); 
then	
 	usage EMPTY
	exit	
fi

case $2 in
	BACKUP )
		if (( $# != 4 )); then
    			usage BACKUP
		fi
		launchTCA $1 $2 $3 $4 ;;
	RESTORE )
		if (( $# != 3 )); then
    			usage RESTORE
		fi
		launchTCA $1 $2 $3 ;;
	SPACERECLAIM )
		if (( $# != 3 )); then
    			usage SPACERECLAIM
		fi
		launchTCA $1 $2 $3 ;;
	STATE )
		if (( $# != 2 )); then
    			usage STATE
		fi
		launchTCA $1 $2 ;;
	DELETE )
		if (( $# != 3 )); then
    			usage DELETE
		fi
		launchTCA $1 $2 $3 ;;
esac

cd ..