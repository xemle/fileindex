package de.silef.service.file.node;

import de.silef.service.file.extension.IndexExtension;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexNodeWriter {

    static int MAGIC_HEADER = 0x23100702;

    public void write(IndexNode root, Path path) throws IOException {
        try (OutputStream output = Files.newOutputStream(path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE_NEW)) {
            write(root, output);
        }
    }

    public void write(IndexNode root, OutputStream output) throws IOException {
        try (DeflaterOutputStream deflaterOutput = new DeflaterOutputStream(output);
             BufferedOutputStream bufferedOutput = new BufferedOutputStream(deflaterOutput);
             DataOutputStream dataOutput = new DataOutputStream(bufferedOutput)) {

            dataOutput.writeInt(MAGIC_HEADER);
            writeNode(root, dataOutput);
        }
    }

    private void writeNode(IndexNode node, DataOutputStream dataOutput) throws IOException {
        dataOutput.writeByte(node.getNodeType().getByte());
        dataOutput.writeUTF(node.getName());

        writeExtensions(node, dataOutput);
        writeChildren(node, dataOutput);
    }

    private void writeExtensions(IndexNode node, DataOutputStream dataOutput) throws IOException {
        List<IndexExtension> extensions = node.getExtensions()
                .stream()
                .sorted(byExtensionType())
                .collect(Collectors.toList());
        dataOutput.writeByte(extensions.size() & 0xff);
        for (IndexExtension extension : extensions) {
            byte[] data = extension.getData();
            dataOutput.writeByte(extension.getType());
            dataOutput.writeShort(data.length);
            dataOutput.write(data);
        }
    }

    private Comparator<IndexExtension> byExtensionType() {
        return (a, b) -> a.getType() - b.getType();
    }

    private void writeChildren(IndexNode node, DataOutputStream output) throws IOException {
        Collection<IndexNode> children = node.getChildren();
        output.writeInt(children.size());
        for (IndexNode child : children) {
            writeNode(child, output);
        }
    }

}
