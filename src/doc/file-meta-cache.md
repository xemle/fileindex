# File Meta Cache

The file meta cache stores file meta data like type, size and timestamp for fast
change lookup of a target path. The meta data is stored in `FileMeta` class.

The `FileMetaCache` can build and compare different file meta data to detect created,
changed, and deleted files.

For The file meta data does provide integrity checks. For Unix systems, the file meta
data contains also the inode which supports to identify changes within the file.

The file meta data is serialized due Java and stored with zlib compression, since 
lot of data are strings from file paths.

# File Format

    +----------------+
    |    4 bytes     |  Header 0x23100702
    +----------------+
    +----------------+
    |    4 bytes     |  Count of file meta items
    +----------------+
    +----------------+
    |    4 bytes     |  File stat, see man 2 stat
    +----------------+
    |    8 bytes     |  File size
    |                |
    +----------------+
    |    8 bytes     |  Created timestamp
    |                |
    +----------------+
    |    8 bytes     |  Modified timestamp
    |                |
    +----------------+
    |    8 bytes     |  inode value or 0
    |                |
    +--------+-------+
    | 2 bytes|       |  Length and path in UTF-8
    +--------+       +
    |     n-bytes    |
    |                |
    +----------------+
    +----------------+
    |      ....      |  Next file meta item
        