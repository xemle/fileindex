# File Index

The file index holds content hashes to files in a hash-tree structure. 

## Format

The file index holds index nodes. A index node is a directory container with lists
of index node entries. Index node entries represent a file in a directory. The index
node is referenced by its content hash. The hash method is SHA-1.

The index node holds a list of children with there content hash, type, and name.
The name of a index node is build by the index node entry reference of its parent.

The order is depth-first. The index node entries are sorted by their names. Lowest
name first. Only directories are listed in the file index.

The first index node is the root node. The root index node has the an empty string 
as name.

The file index is with zlib algorithm compressed.


    +----------------+
    |    4 bytes     |  Header 0x08020305
    +----------------+
    +----------------+
    |    2 bytes     |  Length of index node 
    +----------------+
    |    n bytes     |  Index node content
    /                /
    |                |
    +----------------+
    +----------------+
    |      ....      |  Next index node


An index node is build by its size, followed by a list of index node entries.
A index node entry has a 20 byte SHA-1 hash, one byte type, length of the 
UTF-8 name and the name.
 
    +----------------+
    |    2 bytes     |  Length of index node 
    +----------------+
    +----------------+
    |    20 bytes    |  SHA-1 hash bytes of node entry
    |                |
    |                |
    +----------------+
    |     1 byte     |  Entry type
    |                |
    +----------------+
    |    2 bytes     |  Length of path in UTF-8
    |                |
    +----------------+
    |    n bytes     |  path in UTF-8
    |                |
    |                |
    +----------------+
    +----------------+
    |      ....      |  Next node entry

## Entry Type Values 

1 for directory, 2 for file, 4 for symlink. 0 otherwise. 