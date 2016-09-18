# File Index Cache

The file index cache stores file meta data like type, size and timestamp for fast
change lookup of a target path. The meta data is stored in `IndexNode` class.

The `FileIndex` can build and compare different file indices to detect created,
changed, and deleted files.

The file index data might provide integrity checks. For Unix systems, the file meta
data contains also the inode which supports change identifications within the file.

## File Format

The file index data is serialized and stored with zlib compression. The index data 
is stored in a tree of nodes. A node has its body, count of its children followed by 
its child nodes. Non directory nodes have a child count of 0.

General structure: 

    +----------------+
    |    4 bytes     |  Header 0x23100702
    +----------------+
    +----------------+
    |    n bytes     |  Root node  
    |                |
    +----------------+
    |    4 bytes     |  Children count
    +----------------+
    |    n bytes     |  Child node
    |                |
    +----------------+
    |    n bytes     |  Child node
    |                |
    +----------------+
    |      ...       |


Single index node structure:

    +----------------+
    |    4 bytes     |  File mode
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
    +----------------+
    |   20 bytes     |  Hash value
    |                |
    +----------------+
    |    2 bytes     |  Length of name bytes
    +----------------+
    |    n bytes     |  Name (UTF-8)
    |                |
    +----------------+
    |    4 bytes     |  Children count
    +----------------+
    +----------------+
    |      ....      |  Index node as child node
        
        
## Hash Value

There are two types of hash calculations. For non directory nodes it is simple
the SHA-1 hash of the file content.

For directory nodes, the checksum over the child entries are calculated. The 
children are sorted by their names, lowest name first. Each child
entry contains of its SHA-1 hash, the file mode, and the name in UTF-8.
 
    +----------------+
    |    20 bytes    |  SHA-1 hash bytes of node entry
    |                |
    |                |
    +----------------+
    |    4 bytes     |  File mode
    |                |
    +----------------+
    |    2 bytes     |  Length of name bytes
    +----------------+
    |    n bytes     |  Name (UTF-8)
    |                |
    +----------------+
    +----------------+
    |      ....      |  Next node entry

## Entry File modes

See `man (2) stat` for details. The Enums are defined as follow

    FILE      (0100000),
    LINK      (0120000),
    DIRECTORY (0040000),
    OTHER     (0000000);

Currently the Unix file permissions are not considered.