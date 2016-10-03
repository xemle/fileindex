# File Content Hash

Should the file content hash contain the size of the file? Git file hash
contains object type, size and file content to create a sha1 hash.
 
Pro:

* Improve collision due additional size field

Contra:

* Content hash could not easily reproduced with standard tools lik `sha1sum`
* Create collisions should be very hard by design. The hash should change 
anyway if the size is changing, too

# Dynamic Number Storage

The index file format uses fixed sized number values. Some values require less
bytes in common usecases and a dynamic number storage via extension bit could
represent arbitrary number sizes more efficient. Numbers which might use less
bytes if they are stored dynamically:

* Node children count: Mostly less than 127
* Node name: Average is about 15
* Node Key (aka inode): 0 on non Linux file systems
* Filesize: Avarage 1 MB which requires 3 Bytes
* Extension size: Mostly less than 127

Pro

* Efficient storage

Cons

* Requires more logic and processing
* Might be slower on read and write

# File Key In BasicFileIndexExtension

The inode (and device id) identifies file unique content and hard links. With
the information of inode, better assumptions on file hash calculation and 
deduplication via hard links could be done. However, inodes are only for unix
systems and might be stored in a UnixFileIndexExtension and the question is:
Should the file key be stored in the BasicFileIndexExtension?

Pro

* File key/inode is supported in Java's BasicFileAttribute

Cons

* Reading Unix file attribute is more expensive than reading BasicFileAttribute
(Java creates on unix systems private UnixFileAttribute instances and offers a 
BasicFileAttribute facade)
* Not clear disign. BasicFileIndexExtension should be platform independent