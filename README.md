# File Index Cache

Lightning fast file change lookups with content integrity check. An inspiration of
GIT index.

## Usage

    $ mvn package
    $ java -jar target/fileindex-*-cli.jar [path]
    
The file index is read and written in the given path in the `.fileindex` file.

## Requirements

File Index Cache runs with Java 8 and is build with Maven

## File Format

See [file-index.md](src/doc/file-index.md) in `src/doc` for details.

## Licence

MIT