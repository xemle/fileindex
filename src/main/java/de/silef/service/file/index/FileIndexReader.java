package de.silef.service.file.index;

import de.silef.service.file.meta.FileMode;
import de.silef.service.file.util.HashUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
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

            IndexNode root = null;
            Map<byte[], String> hashToName = new HashMap<>();
            byte[] buf = new byte[1024];

            while (dataInput.available() > 0) {
                int size = dataInput.readInt();
                while (buf.length < size) {
                    buf = new byte[buf.length * 2];
                }
                int read = dataInput.read(buf, 0, size);
                if (read != size) {
                    throw new IOException("Failed to read " + size + " bytes. Got " + read + " bytes");
                }

                byte[] hash = HashUtil.getHash(buf, 0, size);
                if (root == null) {
                    hashToName.put(hash, "");
                }
                IndexNode indexNode = readNode(buf, size, hash, hashToName);
                if (root == null) {
                    root = indexNode;
                }
            }
            return new FileIndex(base, root);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new IOException("Could not read items", e);
        }
    }

    private IndexNode readNode(byte[] buf, int len, byte[] nodeHash, Map<byte[], String> hashToName) {
        int pos = 0;
        List<IndexNode> children = new LinkedList<>();
        while (pos < len) {
            FileMode fileMode = FileMode.create(buf[pos]);
            byte hashLength = buf[pos + 1];
            byte[] hash = Arrays.copyOfRange(buf, pos + 2, pos + 2 + hashLength);
            pos += 2 + hashLength;
            int nameEnd = pos;
            while (nameEnd < len && buf[nameEnd] != 0) {
                nameEnd++;
            }
            String name = new String(buf, pos, nameEnd - pos, StandardCharsets.UTF_8);
            hashToName.put(hash, name);
            children.add(new IndexNode(fileMode, name, hash));
            pos = nameEnd + 1;
        }
        return new IndexNode(hashToName.get(nodeHash), children, nodeHash);
    }


}
