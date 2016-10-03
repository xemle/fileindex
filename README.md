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
    $ java -jar target/fileindex-*-cli.jar -c -u [path]
    
The file index is created and updated from the current directory:

    $ java -jar target/fileindex-1.0.0-SNAPSHOT-cli.jar -c -u
    C  .git/logs/refs/heads/master
    N  .git/objects/17/b0bef62e9eedc172fd81ee818149a0a7206cef
    N  .git/objects/bf/079448c9afd088f5fab951934da55de9ac3cbb
    C  .git/refs/heads/master
    C  .idea/workspace.xml
    R  fileindex.log
    C  src/main/java/de/silef/service/file/FileIndexCli.java
    C  target/classes/de/silef/service/file/util/PathWalker.class
    M  target/classes/simpellogger.properties -> target/classes/simplelogger.properties
    C  target/fileindex-1.0.0-SNAPSHOT-cli.jar

Limit content integrity by `-M` option to save index creation time. Actually, it
can be resumed - never mind :-)

Following command will index the `/home/me/Documents` folder. Content hashes of 
files more than 10 MB are not calculated. 

    $ java -jar target/fileindex-1.0.0-SNAPSHOT-cli.jar -M 10mb /home/me/Documents

For help use `-h` option:

    Following options are available:
     -c,--create                     Create file index from filesystem in not
                                     exist
     -d,--dir <arg>                  Root directory of file index
        --deduplicate                Deduplicate files via hard links based on
                                     the content hashes. If --other-dir is set
                                     the deduplication is performed from
                                     primary dir to the other dir. Requires
                                     indices with --integrity option
        --diff                       Show difference between another index via
                                     --other-dir or --other-index
        --diff-full                  Same as --diff but shows also files of
                                     created or removed directories
     -h,--help                       Print this help
     -i,--index <arg>                Index file path to store. Default is
                                     ~/.cache/fileindex/<dirname>.index
     -I,--index-dir <arg>            Index directory to store file indices.
                                     Default is ~/.cache/fileindex
        --integrity                  Create content hashes
        --integrity-max-size <arg>   Limit content integrity creation by file
                                     size. Use 0 to disable
     -n,--dry-run                    Do not perform any changes
        --other-dir <arg>            Other root directory to create hard links
                                     between two indices
        --other-index <arg>          Other file index to create hard links
                                     between two indices
        --output-limit <arg>         Limit change output printing. Default is
                                     256
     -q,--quiet                      Quiet mode. Do not print output
        --start-delay <arg>          Delays the execution by given seconds.
                                     Useful for profiling
     -u,--update                     Update file index from filesystem
    
    Please consult fileindex.log for detailed program information

## Requirements

File Index Cache runs with Java 8 and is build with Maven

## File Format

See [file-index.md](src/doc/file-index.md) in `src/doc` for details.

## Licence

MIT