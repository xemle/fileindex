package de.silef.service.file.index;

import de.silef.service.file.hash.FileHash;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.InflaterInputStream;

import static de.silef.service.file.index.IndexNode.MAGIC_HEADER;

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
                return new FileIndex(base, IndexNode.createRootFromPath(base));
            } else {
                throw e;
            }
        }
    }

    private FileIndex read(Path base, InputStream input) throws IOException {
        try (InflaterInputStream inflaterInput = new InflaterInputStream(input);
             BufferedInputStream bufferedInput = new BufferedInputStream(inflaterInput);
             DataInputStream dataInput = new DataInputStream(bufferedInput)) {

            int header = dataInput.readInt();
            if (header != MAGIC_HEADER) {
                throw new IOException("Unexpected header: " + header);
            }
            IndexNode root = readNode(null, dataInput);
            return new FileIndex(base, root);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("Could not read cache nodes", e);
        }
    }

    private IndexNode readNode(IndexNode parent, DataInputStream input)
            throws ClassNotFoundException, IOException {

        FileMode mode = FileMode.create(input.readInt());
        long size = input.readLong();
        long creationTime = input.readLong();
        long modifiedTime = input.readLong();
        long inode = input.readLong();

        byte[] buf = new byte[FileHash.LENGTH];
        int read = input.read(buf);
        assert read == FileHash.LENGTH;
        FileHash hash = new FileHash(buf);

        String name = input.readUTF();

        IndexNode node = IndexNode.createFromIndex(parent, mode, size, creationTime, modifiedTime, inode, hash, name);

        int children = input.readInt();
        for (int i = 0; i < children; i++) {
            readNode(node, input);
        }

        return node;
    }
}
