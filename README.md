## Compilation instructions:

### Run RMI
The RMI is created programmatically on the peer 1, thus the peer 1 must start before all the others.

### Run Multicastsnooper
```
java -jar McastSnooper.jar 224.0.0.0:4445 224.0.0.1:4446 224.0.0.2:4447

```

### Run compile.sh in the root of the project, the output of the compile will be found on the build folder
```
./compile.sh 
```

### Launch a peer with the peer.sh script in the build folder (advised to run at least 4 peers)
```
./peer.sh <Version> <Id> <serviceName> <MCip> <MCport> <MDBip> <MDBport> <MDRip> <MDRport>

example:
 ./peer.sh 1.0 1 remoteName 224.0.0.0 4445 224.0.0.1 4446 224.0.0.2 4447

```

## Testing instructions:

### Start Test Client Application (TCA) with the test.sh script in the build folder
```
./test.sh <serviceName> <operation> <operators>
```


## Test BACKUP
```
./test.sh remoteName BACKUP ../TestFiles/Teste.png 3
```
## Test RESTORE
```
./test.sh remoteName RESTORE ../TestFiles/Teste.png
```
## Test DELETE
```
./test.sh remoteName DELETE ../TestFiles/Teste.png 
```
## Test RECLAIM
```
./test.sh remoteName RECLAIM 0
```
## Test STATE
```
./test.sh remoteName STATE
```

