package de.silef.service.file.meta;

import de.silef.service.file.hash.FileHash;
import de.silef.service.file.hash.HashUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileMetaNode implements Serializable {

    static int MAGIC_HEADER = 0x23100702;

    private FileMetaNode parent;
    private List<FileMetaNode> children = new LinkedList<>();

    private String name;

    private FileMode mode;

    private long size;

    private long creationTime;
    private long modifiedTime;

    private long inode;

    private FileHash hash = FileHash.ZERO;

    private FileMetaNode() {
        super();
    }

    public static FileMetaNode createFromIndex(FileMetaNode parent, FileMode mode, long size, long creationTime, long modifiedTime, long inode, FileHash hash, String name) {
        FileMetaNode node = new FileMetaNode();

        node.mode = mode;
        node.size = size;
        node.creationTime = creationTime;
        node.modifiedTime = modifiedTime;
        node.inode = inode;
        node.name = name;
        node.hash = hash;
        node.children = new ArrayList<>();
        node.setParent(parent);
        node.sortChildren();

        return node;
    }

    public static FileMetaNode createRootFromPath(Path file) throws IOException {
        return createFromPath(null, file);
    }

    public static FileMetaNode createFromPath(FileMetaNode parent, Path file) throws IOException {
        if (file == null) {
            throw new NullPointerException("Path must not be null");
        }

        FileMetaNode node = new FileMetaNode();

        node.name = file.getFileName().toString();
        node.size = Files.size(file);

        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);

        node.mode = getMode(attributes);
        node.creationTime = attributes.creationTime().toMillis();
        node.modifiedTime = attributes.lastModifiedTime().toMillis();

        node.inode = readInode(attributes);

        node.setParent(parent);
        node.resetHashesToRootNode();

        return node;
    }

    private static FileMode getMode(BasicFileAttributes attributes) {
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

    private static long readInode(BasicFileAttributes attributes) {
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

    public String getName() {
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

    public void setHash(FileHash hash) {
        assert hash != null : "Hash must not be null";

        this.hash = hash;
    }

    public FileHash getHash() {
        if (hash == null) {
            hash = calculateHash();
        }
        return hash;
    }

    private FileHash calculateHash() {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             DataOutputStream dataOutput = new DataOutputStream(buffer);) {

            for (FileMetaNode child : children) {
                dataOutput.write(child.getHash().getBytes());
                dataOutput.write(child.getMode().getValue());
                dataOutput.writeUTF(child.getName());
            }
            byte[] hash = HashUtil.getHash(buffer.toByteArray());
            return new FileHash(hash);
        } catch (IOException e) {
            throw new RuntimeException("Could not create node hash", e);
        }
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
        return new ArrayList<>(children);
    }

    private void addChild(FileMetaNode node) {
        removeChildByName(node.getName());
        children.add(node);
        sortChildren();
    }

    public FileMetaNode removeChildByName(String name) {
        if (children.isEmpty()) {
            return null;
        }

        int index = findChildIndexByName(name, 0, children.size() - 1);
        if (index < 0) {
            return null;
        }

        FileMetaNode node = children.remove(index);
        sortChildren();
        return node;
    }

    private int findChildIndexByName(String name, int low, int high) {
        int i = low + (high - low);

        FileMetaNode node = children.get(i);
        assert node != null : "Child at " + i + " is null";
        int cmp = node.getName().compareTo(name);

        if (low == high && cmp != 0) {
            return -1;
        } else if (cmp < 0) {
            return findChildIndexByName(name, low, i);
        } else if (cmp > 0) {
            return findChildIndexByName(name, i, high);
        }
        return i;
    }

    private void sortChildren() {
        children.sort((a, b) -> a.getName().compareTo(b.getName()));
    }

    public Path getRelativePath() {
        if (parent != null) {
            return parent.getRelativePath().resolve(name);
        }
        return Paths.get("");
    }

    public void resetHashesToRootNode() {
        hash = null;

        if (parent != null) {
            parent.resetHashesToRootNode();
        }
    }

    public void setParent(FileMetaNode parent) {
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
    }
}
