package de.silef.service.file.index;

import de.silef.service.file.meta.FileMode;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.DeflaterOutputStream;

import static de.silef.service.file.index.FileIndex.MAGIC_HEADER;


/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndexWriter {

    public void write(FileIndex index, Path path) throws IOException {
        try (FileOutputStream output = new FileOutputStream(path.toFile())) {
            write(index, output);
        }
    }

    private void write(FileIndex index, OutputStream output) throws IOException {
        try (DeflaterOutputStream deflaterOutput = new DeflaterOutputStream(output);
             BufferedOutputStream bufferedOutput = new BufferedOutputStream(deflaterOutput);
             DataOutputStream dataOutput = new DataOutputStream(bufferedOutput)) {

            dataOutput.writeInt(MAGIC_HEADER);

            writeNode(index.getRoot(), dataOutput);
        }
    }

    private void writeNode(IndexNode node, DataOutputStream output) throws IOException {
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
             DataOutputStream dataOutput = new DataOutputStream(buf)) {

            node.writeChildren(dataOutput);
            byte[] bytes = buf.toByteArray();

            output.writeShort(bytes.length);
            output.write(bytes);
        }

        writeChildren(node, output);
    }

    private void writeChildren(IndexNode node, DataOutputStream output) throws IOException {
        for (IndexNode child : node.getChildren()) {
            if (child.getFileMode() != FileMode.DIRECTORY) {
                continue;
            }

            writeNode(child, output);
        }
    }

}
