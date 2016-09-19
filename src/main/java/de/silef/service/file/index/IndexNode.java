package de.silef.service.file.index;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexNode implements Serializable {

    static int MAGIC_HEADER = 0x23100702;

    private IndexNode parent = null;
    private List<IndexNode> children = new ArrayList<>();
    private Map<String, IndexNode> nameToChild = new HashMap<>();

    private String name;

    private Path relativePath = null;

    private FileMode mode;

    private long size;

    private long creationTime;
    private long modifiedTime;

    private long inode = 0;

    private FileHash hash = FileHash.ZERO;

    private IndexNode() {
        super();
    }

    public static IndexNode createFromIndex(IndexNode parent, FileMode mode, long size, long creationTime, long modifiedTime, long inode, FileHash hash, String name) {
        IndexNode node = new IndexNode();

        node.mode = mode;
        node.size = size;
        node.creationTime = creationTime;
        node.modifiedTime = modifiedTime;
        node.inode = inode;
        node.name = name;
        node.hash = hash;
        node.parent = parent;

        return node;
    }

    public static IndexNode createRootFromPath(Path file) throws IOException {
        assert Files.isDirectory(file) : "Root must be a directory";

        return createFromPath(file, "");
    }

    public static IndexNode createFromPath(IndexNode parent, Path file) throws IOException {
        assert parent != null : "Parent must not be null";

        IndexNode node = createFromPath(file, file.getFileName().toString());
        node.parent = parent;

        return node;
    }

    private static IndexNode createFromPath(Path file, String name) throws IOException {
        assert file != null : "Path must not be null";
        assert name != null : "Name must not be null";

        IndexNode node = new IndexNode();

        node.size = Files.size(file);

        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);

        node.mode = getMode(attributes);
        node.creationTime = attributes.creationTime().toMillis();
        node.modifiedTime = attributes.lastModifiedTime().toMillis();

        node.inode = readInode(attributes);
        node.hash = FileHash.ZERO;

        node.name = name;

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

    public IndexNode getParent() {
        return parent;
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

            sortChildren();
            for (IndexNode child : children) {
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

        IndexNode that = (IndexNode) o;

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

    public List<IndexNode> getChildren() {
        return new ArrayList<>(children);
    }

    void addChild(IndexNode node) {
        IndexNode oldNode = nameToChild.put(node.getName(), node);
        children.remove(oldNode);
        children.add(node);
    }

    void setChildren(List<IndexNode> children) {
        this.children = new ArrayList<>(children);
        nameToChild = new HashMap<>();
        for (IndexNode node : children) {
            nameToChild.put(node.getName(), node);
        }
    }

    public IndexNode removeChildByName(String name) {
        IndexNode node = findChildByName(name);
        children.remove(node);
        return node;
    }

    public IndexNode findChildByName(String name) {
        return nameToChild.get(name);
    }

    private void sortChildren() {
        children.sort((a, b) -> a.getName().compareTo(b.getName()));
    }

    public Path getRelativePath() {
        if (relativePath == null) {
            relativePath = parent == null ? Paths.get("") : parent.getRelativePath().resolve(name);
        }
        return relativePath;
    }

    void resetHashesToRootNode() {
        if (hash == null) {
            return;
        }

        hash = null;
        if (parent != null) {
            parent.resetHashesToRootNode();
        }
    }

    private void walk(Consumer<IndexNode> consumer) {
        consumer.accept(this);

        for (IndexNode child : children) {
            child.walk(consumer);
        }
    }

    public Stream<IndexNode> stream() {
        Stream.Builder<IndexNode> streamConsumer = Stream.builder();
        walk(streamConsumer);
        return streamConsumer.build();
    }

    void copyFrom(IndexNode other) {
        name = other.getName();

        mode = other.getMode();
        size = other.getSize();
        creationTime = other.getCreationTime();
        modifiedTime = other.getModifiedTime();

        inode = other.getInode();
        hash = other.getHash();
    }
}
