package de.silef.service.file.index;

import de.silef.service.file.hash.FileHash;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

import static de.silef.service.file.index.IndexNode.MAGIC_HEADER;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexNodeReader {

    public IndexNode read(Path base, Path file) throws IOException {
        return read(base, file, false);
    }

    public IndexNode read(Path base, Path file, boolean suppressWarning) throws IOException {
        try {
            try (InputStream input = new FileInputStream(file.toFile())) {
                return read(input);
            }
        } catch (IOException e) {
            if (suppressWarning) {
                return IndexNode.createRootFromPath(base);
            } else {
                throw e;
            }
        }
    }

    private IndexNode read(InputStream input) throws IOException {
        try (InflaterInputStream inflaterInput = new InflaterInputStream(input);
             BufferedInputStream bufferedInput = new BufferedInputStream(inflaterInput);
             DataInputStream dataInput = new DataInputStream(bufferedInput)) {

            int header = dataInput.readInt();
            if (header != MAGIC_HEADER) {
                throw new IOException("Unexpected header: " + header);
            }
            return readNode(null, dataInput);
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

        int childrenCount = input.readInt();
        List<IndexNode> children = new ArrayList<>(childrenCount);
        for (int i = 0; i < childrenCount; i++) {
            children.add(readNode(node, input));
        }
        node.setChildren(children);
        return node;
    }
}
