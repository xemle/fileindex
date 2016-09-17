package de.silef.service.file.index;

import de.silef.service.file.meta.FileMeta;
import de.silef.service.file.meta.FileMetaCache;
import de.silef.service.file.meta.FileMode;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import static de.silef.service.file.index.FileIndex.MAGIC_HEADER;


/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndexReader {
    public FileIndex read(Path base, Path file) throws IOException {
        return read(base, file, false);
    }

    public FileIndex read(Path base, Path file, boolean suppressWarning) throws IOException {
        try {
            try (InputStream input = new FileInputStream(file.toFile())) {
                return read(base, input);
            }
        } catch (IOException e) {
            if (suppressWarning) {
                return new FileIndex(base, new HashMap<>());
            } else {
                throw e;
            }
        }
    }

    private FileIndex read(Path base, InputStream input) throws IOException {
        try (InflaterInputStream inflaterInput = new InflaterInputStream(input);
             BufferedInputStream bufferedInput = new BufferedInputStream(inflaterInput);
             DataInputStream objectInput = new DataInputStream(bufferedInput)) {

            Map<String, byte[]> pathToHash = new HashMap<>();

            int header = objectInput.readInt();
            if (header != MAGIC_HEADER) {
                throw new IOException("Unexpected header: " + header);
            }
            int size = objectInput.readInt();
            for (int i = 0; i < size; i++) {
                readPathHash(objectInput, pathToHash);
            }
            return new FileIndex(base, pathToHash);
        } catch (ClassCastException e) {
            throw new IOException("Could not read items", e);
        }
    }

    private void readPathHash(DataInputStream objectInput, Map<String, byte[]> pathToHash) throws IOException {
        int length = objectInput.readByte();
        byte[] hash = new byte[length];
        int read = objectInput.read(hash);
        if (read != length) {
            throw new IOException("Could not read hash. Expected " + length + " bytes but got " + read);
        }
        String path = objectInput.readUTF();
        pathToHash.put(path, hash);
    }

}
