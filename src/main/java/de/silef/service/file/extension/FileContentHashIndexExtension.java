package de.silef.service.file.extension;

import de.silef.service.file.util.DataUtils;
import de.silef.service.file.util.HashUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Created by sebastian on 23.09.16.
 */
public class FileContentHashIndexExtension extends StandardIndexExtension {

    private static final int DATA_SIZE = 20;

    public FileContentHashIndexExtension(byte[] data) {
        super(ExtensionType.FILE_HASH.value, data);
        if (data.length < DATA_SIZE) {
            throw new IndexExtensionInvalidDataException("Invalid data length for Content Extension");
        }
    }

    public static FileContentHashIndexExtension create(Path file) throws IOException {
        if (!Files.isSymbolicLink(file) && !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("File content hash is only allowed on files and symbolic links");
        }
        long size = Files.size(file);
        byte[] sizeBytes = DataUtils.toBytes(size);

        try (InputStream sizeInput = new ByteArrayInputStream(sizeBytes);
             InputStream fileInput = getFileInputStream(file);
             InputStream sequenceInput = new SequenceInputStream(sizeInput, fileInput)) {

            byte[] hash = HashUtil.getHash(sequenceInput);

            return new FileContentHashIndexExtension(hash);
        }
    }

    private static InputStream getFileInputStream(Path file) throws IOException {
        if (Files.isSymbolicLink(file)) {
            Path target = Files.readSymbolicLink(file);
            return new ByteArrayInputStream(target.toString().getBytes(StandardCharsets.UTF_8));
        } else {
            InputStream fileInput = Files.newInputStream(file, StandardOpenOption.READ);
            return new BufferedInputStream(fileInput, 1 << 15);
        }
    }

    @Override
    public String toString() {
        return "FileContentHash{" + HashUtil.toHex(getData()) + "}";
    }
}
