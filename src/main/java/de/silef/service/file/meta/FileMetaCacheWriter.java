package de.silef.service.file.meta;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.zip.DeflaterOutputStream;

import static de.silef.service.file.meta.FileMeta.MAGIC_HEADER;

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

            Collection<FileMeta> items = cache.getFileMetaItems();

            dataOutput.writeInt(MAGIC_HEADER);
            dataOutput.writeInt(items.size());
            for (FileMeta item : items) {
                writeObject(item, dataOutput);
            }
        }
    }

    private void writeObject(FileMeta fileMeta, DataOutputStream output)
            throws IOException {
        output.writeInt(fileMeta.getMode().getValue());
        output.writeLong(fileMeta.getSize());
        output.writeLong(fileMeta.getCreationTime());
        output.writeLong(fileMeta.getModifiedTime());
        output.writeLong(fileMeta.getInode());

        output.writeUTF(fileMeta.getPath());
    }

}
