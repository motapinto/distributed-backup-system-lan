## Compilation instructions:

### Run RMI
The RMI is created programmatically on the peer 1, thus the peer 1 must be always active. No need to do anything here.

### Run Multicastsnooper
```
java -jar McastSnooper.jar 224.0.0.0:4445 224.0.0.1:4446 224.0.0.2:4447
```

### Launch peers script
```
bash peers.sh <Number of Peers> <Version> <MCip> <MCport> <MDBip> <MDBport> <MDRip> <MDRport>
```

## Testing instructions:


### Start Test Client Application (TCA)
```
bash tca.sh <Access Point> <Protocol> <operand1> <operand2>
```


To start the peer run the following command(advised to run at least 3 peers):
```
java Peer.InitPeer 1.0 1 224.0.0.0 4445 224.0.0.1 4446 224.0.0.2 4447
```

## Test BACKUP
```
java TestingClientApplication.TCA 1 BACKUP 300kb.pdf 2
```
## Test RESTORE
```
java TestingClientApplication.TCA 1 RESTORE lbaw.pdf
```
## Test DELETE
```
java TestingClientApplication.TCA 1 DELETE lbaw.pdf
```
## Test STATE
```
java TestingClientApplication.TCA 1 STATE
```

**Disclaimer** -  The scripts may not work on Windows. If that is the case you should install the Windows subsystem for Linux
