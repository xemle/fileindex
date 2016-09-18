package de.silef.service.file.index;

import de.silef.service.file.meta.FileMode;
import de.silef.service.file.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.InflaterInputStream;

import static de.silef.service.file.index.FileIndex.MAGIC_HEADER;
import static de.silef.service.file.util.HashUtil.HASH_LEN;


/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndexReader {

    private static final Logger LOG = LoggerFactory.getLogger(FileIndexReader.class);

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
                return new FileIndex(base);
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

            IndexNode root = readIndexRoot(dataInput);

            return new FileIndex(base, root);
        } catch (ClassCastException | IllegalArgumentException | IndexOutOfBoundsException e) {
            throw new IOException("Could not read cache node", e);
        }
    }

    private IndexNode readIndexRoot(DataInputStream dataInput) throws IOException {
        Map<String, IndexNode> hashToNode = new HashMap<>();
        byte[] buf = new byte[1024];
        IndexNode root = null;

        while (dataInput.read(buf, 0, 4) == 4) {
            int size = readInt(buf, 0);
            buf = resizeBuffer(buf, size);
            int read = dataInput.read(buf, 0, size);
            if (read != size) {
                throw new IOException("Failed to read " + size + " bytes. Got " + read + " bytes");
            }

            byte[] hash = HashUtil.getHash(buf, 0, size);
            root = readNode(buf, size, hash, hashToNode);
            hashToNode.put(HashUtil.toHex(hash), root);
        }

        return root;
    }

    private IndexNode readNode(byte[] buf, int bufLen, byte[] nodeHash, Map<String, IndexNode> hashToNode) {
        List<IndexNode> children = new LinkedList<>();

        int pos = 0;
        while (pos < bufLen) {
            byte[] hash = Arrays.copyOfRange(buf, pos, pos + HASH_LEN);
            pos += HASH_LEN;
            FileMode fileMode = FileMode.create(buf[pos]);
            pos += 1;
            int length = readShort(buf, pos);
            pos += 2;
            String name = new String(buf, pos, length, StandardCharsets.UTF_8);
            pos += length;

            IndexNode child;
            if (fileMode == FileMode.DIRECTORY) {
                child = hashToNode.get(HashUtil.toHex(hash));
                child.setName(name);
            } else {
                child = new IndexNode(fileMode, name, hash);
            }
            children.add(child);
        }

        return new IndexNode(children, nodeHash);
    }

    private int readInt(byte[] buf, int offset) {
        return ((buf[offset] & 0xff) << 24) +
               ((buf[offset + 1] & 0xff) << 16) +
               ((buf[offset + 2] & 0xff) << 8) +
               ((buf[offset + 3] & 0xff));
    }

    private int readShort(byte[] buf, int offset) {
        return ((buf[offset] & 0xff) << 8) + (buf[offset + 1] & 0xff);
    }

    private byte[] resizeBuffer(byte[] buf, int size) {
        byte[] result = buf;
        while (result.length < size) {
            result = new byte[result.length * 2];
        }
        return result;
    }

    private String getNodeName(byte[] nodeHash, Map<String, IndexNode> hashToNode) {
        String result;
        if (hashToNode.isEmpty()) {
            result = "";
        } else {
            result = hashToNode.get(HashUtil.toHex(nodeHash)).getName();
        }
        return result;
    }

}
