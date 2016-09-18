package de.silef.service.file.index;

import de.silef.service.file.meta.FileMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.DeflaterOutputStream;

import static de.silef.service.file.index.FileIndex.MAGIC_HEADER;


/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndexWriter {

    private static final Logger LOG = LoggerFactory.getLogger(FileIndexWriter.class);

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
        writeChildren(node, output);

        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             DataOutputStream dataOutput = new DataOutputStream(buffer)) {

            node.writeChildren(dataOutput);
            byte[] bytes = buffer.toByteArray();

            output.writeInt(bytes.length);
            output.write(bytes);
        }
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
