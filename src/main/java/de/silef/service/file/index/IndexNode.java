package de.silef.service.file.index;

import de.silef.service.file.meta.FileMode;
import de.silef.service.file.util.HashUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexNode {

    private FileMode fileMode;
    private String name;
    private byte[] hash;

    private List<IndexNode> children;

    IndexNode(String name, byte[] hash) {
        this.fileMode = FileMode.FILE;
        this.name = name;
        this.hash = hash;
        children = new ArrayList<>();
    }

    IndexNode(List<IndexNode> children, byte[] hash) {
        fileMode = FileMode.DIRECTORY;
        this.name = "";
        this.hash = hash;
        this.children = children;
    }

    IndexNode(String name, List<IndexNode> children) {
        fileMode = FileMode.DIRECTORY;
        this.name = name;
        this.children = children;
        hash = null;
    }

    IndexNode(String name, List<IndexNode> children, byte[] hash) {
        fileMode = FileMode.DIRECTORY;
        this.name = name;
        this.children = children;
        this.hash = hash;
    }

    IndexNode(FileMode fileMode, String name, byte[] hash) {
        this.fileMode = fileMode;
        this.name = name;
        this.hash = hash;
        this.children = new ArrayList<>();
    }

    void writeChildren(DataOutputStream output) throws IOException {
        if (fileMode != FileMode.DIRECTORY) {
            return;
        }
        for (IndexNode child : children) {
            output.write(child.getHash());
            output.write(child.getFileMode().getValue());
            output.writeUTF(child.getName());
        }
    }

    FileMode getFileMode() {
        return fileMode;
    }

    String getName() {
        return name;
    }

    byte[] getHash() throws IOException {
        if (hash == null) {
            try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                DataOutputStream dataOutput = new DataOutputStream(output);) {
                writeChildren(dataOutput);
                hash = HashUtil.getHash(output.toByteArray());
            }
        }
        return hash;
    }

    List<IndexNode> getChildren() {
        return children;
    }

    IndexNode findChildren(String name) {
        for (IndexNode child : children) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }
}
