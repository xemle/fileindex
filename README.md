# File Index Cache

Lightning fast file change lookups with content integrity check. An inspiration of
GIT index.

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

For help use `-h` option:
 
    $ java -jar target/fileindex-1.0.0-SNAPSHOT-cli.jar -h
    usage: fileindex <options> [path]
    
    Following options are available:
     -h         Print this help
     -i <arg>   Index file to store. Default is
                ~/.cache/filecache/<dirname>.index
     -q         Quiet mode

Please consult fileindex.log for detailed program information

## Requirements

File Index Cache runs with Java 8 and is build with Maven

## File Format

See [file-index.md](src/doc/file-index.md) in `src/doc` for details.

## Licence

MIT