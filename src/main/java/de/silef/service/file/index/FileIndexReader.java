package de.silef.service.file.index;

import de.silef.service.file.meta.FileMode;
import de.silef.service.file.util.HashUtil;

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

            IndexNode root = readIndexNode(dataInput);

            return new FileIndex(base, root);
        } catch (ClassCastException | IllegalArgumentException | IndexOutOfBoundsException e) {
            throw new IOException("Could not read items", e);
        }
    }

    private IndexNode readIndexNode(DataInputStream dataInput) throws IOException {
        Map<String, String> hashToName = new HashMap<>();
        byte[] buf = new byte[1024];
        IndexNode root = null;

        while (dataInput.read(buf, 0, 2) == 2) {
            int size = readShort(buf);
            buf = resizeBuffer(buf, size);
            int read = dataInput.read(buf, 0, size);
            if (read != size) {
                throw new IOException("Failed to read " + size + " bytes. Got " + read + " bytes");
            }

            byte[] nodeHash = HashUtil.getHash(buf, 0, size);
            IndexNode indexNode = readChildren(buf, size, nodeHash, hashToName);
            if (root == null) {
                root = indexNode;
            }
        }
        return root;
    }

    private IndexNode readChildren(byte[] buf, int bufLen, byte[] nodeHash, Map<String, String> hashToName) {
        String nodeName = getNodeName(nodeHash, hashToName);
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

            children.add(new IndexNode(fileMode, name, hash));
            hashToName.put(HashUtil.toHex(hash), name);
        }

        return new IndexNode(nodeName, children, nodeHash);
    }

    private int readShort(byte[] buf) {
        return readShort(buf, 0);
    }

    private int readShort(byte[] buf, int offset) {
        return (buf[offset] << 8) + (buf[offset + 1]);
    }

    private byte[] resizeBuffer(byte[] buf, int size) {
        byte[] result = buf;
        while (result.length < size) {
            result = new byte[result.length * 2];
        }
        return result;
    }

    private String getNodeName(byte[] nodeHash, Map<String, String> hashToName) {
        String result;
        if (hashToName.isEmpty()) {
            result = "";
        } else {
            result = hashToName.get(HashUtil.toHex(nodeHash));
        }
        return result;
    }

}
