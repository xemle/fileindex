package de.silef.service.file.meta;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileMetaNode implements Serializable {

    static int MAGIC_HEADER = 0x23100702;

    private FileMetaNode parent;
    private List<FileMetaNode> children = new LinkedList<>();

    private Path name;

    private FileMode mode;

    private long size;

    private long creationTime;
    private long modifiedTime;

    private long inode;

    public FileMetaNode(FileMetaNode parent, FileMode mode, long size, long creationTime, long modifiedTime, long inode, Path name) {
        setParent(parent);
        this.mode = mode;
        this.size = size;
        this.creationTime = creationTime;
        this.modifiedTime = modifiedTime;
        this.inode = inode;
        this.name = name;
    }

    private void addChild(FileMetaNode node) {
        children.add(node);
    }

    public FileMetaNode(FileMetaNode parent, Path file) throws IOException {
        if (file == null) {
            throw new NullPointerException("Path must not be null");
        }
        setParent(parent);

        this.name = file.getFileName();

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

    public Path getName() {
        return name;
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

        FileMetaNode that = (FileMetaNode) o;

        if (inode != that.inode) return false;
        if (modifiedTime != that.modifiedTime) return false;
        if (creationTime != that.creationTime) return false;

        if (size != that.size) return false;
        if (mode != that.mode) return false;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = mode.value;
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (creationTime ^ (creationTime >>> 32));
        result = 31 * result + (int) (modifiedTime ^ (modifiedTime >>> 32));
        result = 31 * result + (int) (inode ^ (inode >>> 32));
        result = 31 * result + name.hashCode();
        return result;
    }

    public List<FileMetaNode> getChildren() {
        return children;
    }

    public Path getRelativePath() {
        if (parent != null) {
            return parent.getRelativePath().resolve(name);
        }
        return Paths.get("");
    }

    public void setParent(FileMetaNode parent) {
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
    }
}
