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
    |    n bytes     |  Root index node
    |                |
    +----------------+
    |    4 bytes     |  Children count
    +----------------+
    |    n bytes     |  Child index node
    |                |
    +----------------+
    |    n bytes     |  Child index node
    |                |
    +----------------+
    |      ...       |

Single index node structure:

    +----------------+
    |    1 byte      |  Node type
    +----------------+
    |    2 bytes     |  Length of name bytes
    +----------------+
    |    n bytes     |  Name (UTF-8)
    |                |
    +----------------+
    |    1 byte      |  Extension count
    +----------------+
    |    n byte      |  Extensions
    +----------------+
    |    4 bytes     |  Children count, might be 0
    +----------------+
    +----------------+
    |      ....      |  Index node as child node


Index node types

The index node type is one byte and defined as followed:

    DIRECTORY 0x01
    FILE      0x02
    LINK      0x04
    OTHER     0x00

## Extension structure

    +----------------+
    |    1 byte      |  Extension type
    +----------------+
    |    2 bytes     |  Length of data
    +----------------+
    |    n bytes     |  Extension data
    |                |
    +----------------+

### Basic File Extension

    +----------------+
    |    1 byte      |  Extension type = 0x01
    +----------------+
    |    2 bytes     |  Length of data = 32
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
    |    8 bytes     |  File key
    |                |
    +----------------+

For Unix filesystems the file key is the inode

### Unix File Extension:

    +----------------+
    |    1 byte      |  Extension type = 0x02
    +----------------+
    |    2 bytes     |  Length of data = 24
    +----------------+
    |    4 bytes     |  File mode
    |                |
    +----------------+
    |    4 bytes     |  User id
    |                |
    +----------------+
    |    4 bytes     |  Group id
    |                |
    +----------------+
    |    8 bytes     |  Inode number
    |                |
    +----------------+
    |    4 bytes     |  Link count
    |                |
    +----------------+

### File Content Hash

    +----------------+
    |    1 byte      |  Extension type = 0x03
    +----------------+
    |    2 bytes     |  Length of data = 20
    +----------------+
    |   20 bytes     |  SHA1 Hash
    |                |
    +----------------+

The File Content Hash is only file and symbolic links (including
symbolic links of directories).

The SHA1 hash is calculated by 8 bytes file size followed by the file 
content. The file content for symbolic link is their link content.

### Universal Hash

    +----------------+
    |    1 byte      |  Extension type = 0x04
    +----------------+
    |    2 bytes     |  Length of data = 20
    +----------------+
    |   20 bytes     |  SHA1 Hash
    |                |
    +----------------+

The universal hash is only for directory index node types `0x01` (no symbolic 
links). The universal hash is system and timestamp independent and offers
a verification hash of file size, file content, and their directory structure. 
The universal hash of a Windows directory has the same hash value as a Linux
directory.
  
The SHA1 hash is calculated by the SHA1 hashes of the child nodes. The nodes
are sorted by their names. For each child the hash data contains hash, node
type and child name. If the child is a directory, the hash value is also a
Universal hash. Otherwise it is the file content hash. If no file content
hash extension is available, a zero hash value (`00000000000000000000` 
as string) is assumed.

    +----------------+
    |   20 byte      |  Child hash
    +----------------+
    |    1 bytes     |  Index node type
    +----------------+
    |    2 bytes     |  Length of name bytes
    +----------------+
    |    n bytes     |  Name (UTF-8)
    |                |
    +----------------+

