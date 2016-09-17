package de.silef.service.file.index;

import de.silef.service.file.meta.FileMode;
import de.silef.service.file.util.HashUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexNode {

    private FileMode fileMode;
    private String name;
    private byte[] hash;
    private byte[] lazyBytes;

    private List<IndexNode> children;

    public IndexNode(String name, byte[] hash) {
        this.fileMode = FileMode.FILE;
        this.name = name;
        this.hash = hash;
        children = new ArrayList<>();
    }

    public IndexNode(String name, List<IndexNode> children) throws IOException {
        fileMode = FileMode.DIRECTORY;
        this.name = name;
        this.children = children;
        hash = buildDirectoryHash();
    }

    public IndexNode(String name, List<IndexNode> children, byte[] hash) {
        fileMode = FileMode.DIRECTORY;
        this.name = name;
        this.children = children;
        this.hash = hash;
    }

    public IndexNode(FileMode fileMode, String name, byte[] hash) {
        this.fileMode = fileMode;
        this.name = name;
        this.hash = hash;
        this.children = new ArrayList<>();
    }

    private byte[] buildDirectoryHash() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(output);
        return HashUtil.getHash(output.toByteArray());
    }

    void write(OutputStream output) throws IOException {
        if (fileMode != FileMode.DIRECTORY) {
            return;
        }
        for (IndexNode node : children) {
            output.write(node.fileMode.getValue());
            output.write((byte) (0xff & node.hash.length));
            output.write(node.hash);
            byte[] nameBytes = node.name.getBytes(StandardCharsets.UTF_8);
            output.write(nameBytes);
            output.write(0);
        }
    }

    public FileMode getFileMode() {
        return fileMode;
    }

    public String getName() {
        return name;
    }

    public byte[] getHash() {
        return hash;
    }

    public List<IndexNode> getChildren() {
        return children;
    }

    public IndexNode findChildren(String name) {
        for (IndexNode child : children) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }
}
