package de.silef.service.file.index;

import de.silef.service.file.meta.FileMeta;

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

            Map<String, byte[]> items = index.getPathToHash();

            dataOutput.writeInt(MAGIC_HEADER);
            dataOutput.writeInt(items.size());
            for (Map.Entry<String, byte[]> item : items.entrySet()) {
                byte[] hash = item.getValue();
                dataOutput.writeByte((byte) (0xff & hash.length));
                dataOutput.write(hash);
                dataOutput.writeUTF(item.getKey());
            }
        }
    }

}
