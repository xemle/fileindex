package de.silef.service.file.meta;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.InflaterInputStream;

import static de.silef.service.file.meta.FileMeta.MAGIC_HEADER;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileMetaCacheReader {


    public FileMetaCache read(Path base, Path file) throws IOException {
        return read(base, file, false);
    }

    public FileMetaCache read(Path base, Path file, boolean suppressWarning) throws IOException {
        try {
            try (InputStream input = new FileInputStream(file.toFile())) {
                return read(base, input);
            }
        } catch (IOException e) {
            if (suppressWarning) {
                return new FileMetaCache(base, new HashMap<>());
            } else {
                throw e;
            }
        }
    }

    private FileMetaCache read(Path base, InputStream input) throws IOException {
        try (InflaterInputStream inflaterInput = new InflaterInputStream(input);
             BufferedInputStream bufferedInput = new BufferedInputStream(inflaterInput);
             DataInputStream objectInput = new DataInputStream(bufferedInput)) {

            Map<String, FileMeta> cache = new HashMap<>();

            int header = objectInput.readInt();
            if (header != MAGIC_HEADER) {
                throw new IOException("Unexpected header: " + header);
            }
            int size = objectInput.readInt();
            for (int i = 0; i < size; i++) {
                FileMeta meta = readObject(objectInput);
                cache.put(meta.getPath(), meta);
            }
            return new FileMetaCache(base, cache);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("Could not read items", e);
        }
    }

    private FileMeta readObject(DataInputStream input)
            throws ClassNotFoundException, IOException {

        FileMode mode = FileMode.create(input.readInt());
        long size = input.readLong();
        long creationTime = input.readLong();
        long modifiedTime = input.readLong();
        long inode = input.readLong();

        String path = input.readUTF();

        return new FileMeta(mode, size, creationTime, modifiedTime, inode, path);
    }
}
