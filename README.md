# File Index Cache

Lightning fast file change lookups with content integrity verification

>  A tribute to GIT index

## Motivation
 
Everyone works with files and we are depending heavily on their integrity and 
freshness. Which file was changed (uninformed)? Are there any updates? Do I need
to get a fresh copy? Services like DropBox and Google Drive, Backup systems and 
virus detections, thumbnail generations and Desktop search engines, etc. all need 
to know what is happening with the user's files.

File change detection and integrity checks are fundamental requirements for 
almost every system. Time to answer these questions can be crucial, too. E.g. is
the data still up-to-date after the time consuming backup or upload procedure 
finished?

File Index Cache answers these questions fast and correct. It builds a fast 
lookup index of a full directory structure. The changes are detected by comparing
a persisted index with the current filesystem state. Filesystem information like
file size, modification timestamps or inodes on Unix systems help to get very good 
results.

Furthermore, the files and directory structure can be verified - securely. Checksums of
files and directories from files leaves up to the root directory verifies that no 
single bit changed. The fingerprint of the root hash tree summarizes the cryptographic 
locked state down to just a few bytes. The root hash can be calculated and verified
on other copies. For synchronization proposes the hash tree can cut-off huge common 
sub directories and only changed folders and files can be detected and updated.

There are sooo many use cases for this file index cache...

## Usage

    $ mvn package
    $ java -jar target/fileindex-*-cli.jar [path]
    
The file index is read and written in the given path in the `.fileindex` file.

    $ java -jar target/fileindex-1.0.0-SNAPSHOT-cli.jar 
    M  .git/logs/refs/heads/master
    C  .git/objects/17/b0bef62e9eedc172fd81ee818149a0a7206cef
    C  .git/objects/bf/079448c9afd088f5fab951934da55de9ac3cbb
    M  .git/refs/heads/master
    M  .idea/workspace.xml
    R  fileindex.log
    M  src/main/java/de/silef/service/file/FileIndexCli.java
    M  target/classes/de/silef/service/file/util/PathWalker.class
    M  target/classes/simplelogger.properties
    M  target/fileindex-1.0.0-SNAPSHOT-cli.jar

Limit content integrity by `-M` option to save index creation time. Actually, it
can be resumed - never mind :-)

Following command will index the `/home/me/Documents` folder. Content hashes of 
files more than 10 MB are not calculated. 

    $ java -jar target/fileindex-1.0.0-SNAPSHOT-cli.jar -M 10mb /home/me/Documents

For help use `-h` option:
 
    usage: fileindex <options> [path]

    Following options are available:
     -h                           Print this help
     -i <arg>                     Index file to store. Default is
                                  ~/.cache/fileindex/<dirname>.index
     -M,--verify-max-size <arg>   Limit content integrity verification by file
                                  size. Use 0 to disable
     -n                           Print changes only. Requires an existing
                                  file index
        --output-limit <arg>      Limit change output printing. Default is 256
     -q                           Quiet mode
    
    Please consult fileindex.log for detailed program information

## Requirements

File Index Cache runs with Java 8 and is build with Maven

## File Format

See [file-index.md](src/doc/file-index.md) in `src/doc` for details.

## Licence

MIT