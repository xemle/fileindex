package de.silef.service.file.meta;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileMeta implements Serializable {

    static int MAGIC_HEADER = 0x23100702;

    private String path;

    private FileMode mode;

    private long size;

    private long creationTime;
    private long modifiedTime;

    private long inode;

    public FileMeta(FileMode mode, long size, long creationTime, long modifiedTime, long inode, String path) {
        this.mode = mode;
        this.size = size;
        this.creationTime = creationTime;
        this.modifiedTime = modifiedTime;
        this.inode = inode;
        this.path = path;
    }

    public FileMeta(Path file, String path) throws IOException {
        if (path == null) {
            throw new NullPointerException("Path must not be null");
        }
        this.path = path;

        size = Files.size(file);

        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);

        mode = getMode(attributes);
        creationTime = attributes.creationTime().toMillis();
        modifiedTime = attributes.lastModifiedTime().toMillis();

        inode = readInode(attributes);
    }

    private FileMode getMode(BasicFileAttributes attributes) {
        if (attributes.isRegularFile()) {
            return FileMode.FILE;
        } else if (attributes.isDirectory()) {
            return FileMode.DIRECTORY;
        } else if (attributes.isSymbolicLink()) {
            return FileMode.LINK;
        } else {
            return FileMode.OTHER;
        }
    }

    private long readInode(BasicFileAttributes attributes) {
        Object key = attributes.fileKey();
        if (key != null) {
            String value = key.toString();
            int start = value.indexOf("ino=");
            if (start >= 0) {
                start += 4;
                int end = start;
                while (end < value.length()) {
                    char c = value.charAt(end);
                    if (c < '0' || c > '9') {
                        break;
                    }
                    end++;
                }
                if (start < end) {
                    return Long.parseLong(value.substring(start, end));
                }
            }
        }
        return 0;
    }

    public String getPath() {
        return path;
    }

    public FileMode getMode() {
        return mode;
    }

    public long getSize() {
        return size;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public long getInode() {
        return inode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileMeta that = (FileMeta) o;

        if (inode != that.inode) return false;
        if (modifiedTime != that.modifiedTime) return false;
        if (creationTime != that.creationTime) return false;

        if (size != that.size) return false;
        if (mode != that.mode) return false;

        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + mode.value;
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (creationTime ^ (creationTime >>> 32));
        result = 31 * result + (int) (modifiedTime ^ (modifiedTime >>> 32));
        result = 31 * result + (int) (inode ^ (inode >>> 32));
        return result;
    }

}
