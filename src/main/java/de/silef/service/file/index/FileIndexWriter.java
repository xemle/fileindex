package de.silef.service.file.index;

import de.silef.service.file.meta.FileMeta;
import de.silef.service.file.meta.FileMode;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
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

            IndexNode root = index.getRoot();

            dataOutput.writeInt(MAGIC_HEADER);
            writeChildren(root, dataOutput);
        }
    }

    private void writeChildren(IndexNode node, DataOutputStream output) throws IOException {
        for (IndexNode child : node.getChildren()) {
            if (child.getFileMode() != FileMode.DIRECTORY) {
                continue;
            }

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            child.write(buf);
            byte[] bytes = buf.toByteArray();
            output.writeInt(bytes.length);
            output.write(bytes);

            writeChildren(child, output);
        }
    }

}
