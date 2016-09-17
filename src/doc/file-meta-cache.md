# File Meta Cache

The file meta cache stores file meta data like type, size and timestamp for fast
change lookup of a target path. The meta data is stored in `FileMeta` class.

The `FileMetaCache` can build and compare different file meta data to detect created,
changed, and deleted files.

For The file meta data does provide integrity checks. For Unix systems, the file meta
data contains also the inode which supports to identify changes within the file.

The file meta data is serialized due Java and stored with zlib compression, since 
lot of data are strings from file paths.