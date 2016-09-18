package de.silef.service.file.meta;

import de.silef.service.file.hash.FileHash;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import static de.silef.service.file.meta.FileMetaNode.MAGIC_HEADER;

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
                return new FileMetaCache(base, new FileMetaNode(null, base));
            } else {
                throw e;
            }
        }
    }

    private FileMetaCache read(Path base, InputStream input) throws IOException {
        try (InflaterInputStream inflaterInput = new InflaterInputStream(input);
             BufferedInputStream bufferedInput = new BufferedInputStream(inflaterInput);
             DataInputStream dataInput = new DataInputStream(bufferedInput)) {

            int header = dataInput.readInt();
            if (header != MAGIC_HEADER) {
                throw new IOException("Unexpected header: " + header);
            }
            FileMetaNode root = readNode(null, dataInput);
            return new FileMetaCache(base, root);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("Could not read cache nodes", e);
        }
    }

    private FileMetaNode readNode(FileMetaNode parent, DataInputStream input)
            throws ClassNotFoundException, IOException {

        FileMode mode = FileMode.create(input.readInt());
        long size = input.readLong();
        long creationTime = input.readLong();
        long modifiedTime = input.readLong();
        long inode = input.readLong();

        byte[] buf = new byte[FileHash.LENGTH];
        assert input.read(buf) == FileHash.LENGTH;
        FileHash hash = new FileHash(buf);

        Path path = Paths.get(input.readUTF());


        FileMetaNode node = new FileMetaNode(parent, mode, size, creationTime, modifiedTime, inode, path, hash);

        int children = input.readInt();
        for (int i = 0; i < children; i++) {
            readNode(node, input);
        }

        return node;
    }
}
