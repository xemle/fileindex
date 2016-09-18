package de.silef.service.file.meta;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.zip.DeflaterOutputStream;

import static de.silef.service.file.meta.FileMetaNode.MAGIC_HEADER;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileMetaCacheWriter {

    public void write(FileMetaCache cache, Path path) throws IOException {
        try (FileOutputStream output = new FileOutputStream(path.toFile())) {
            write(cache, output);
        }
    }

    public void write(FileMetaCache cache, OutputStream output) throws IOException {
        try (DeflaterOutputStream deflaterOutput = new DeflaterOutputStream(output);
             BufferedOutputStream bufferedOutput = new BufferedOutputStream(deflaterOutput);
             DataOutputStream dataOutput = new DataOutputStream(bufferedOutput)) {

            FileMetaNode root = cache.getRoot();

            dataOutput.writeInt(MAGIC_HEADER);
            writeNode(root, dataOutput);
        }
    }

    private void writeNode(FileMetaNode node, DataOutputStream output)
            throws IOException {
        output.writeInt(node.getMode().getValue());
        output.writeLong(node.getSize());
        output.writeLong(node.getCreationTime());
        output.writeLong(node.getModifiedTime());
        output.writeLong(node.getInode());
        output.write(node.getHash().getBytes());

        output.writeUTF(node.getName());

        Collection<FileMetaNode> children = node.getChildren();
        output.writeInt(children.size());
        for (FileMetaNode child : children) {
            writeNode(child, output);
        }
    }

}
