package de.silef.service.file.extension;

import de.silef.service.file.util.DataUtils;
import de.silef.service.file.util.HashUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

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
        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isSymbolicLink() && !attributes.isRegularFile()) {
            throw new IllegalArgumentException("File content hash is only allowed on files and symbolic links: " + file);
        }
        long size = attributes.isSymbolicLink() ? 0L : attributes.size();
        byte[] sizeBytes = DataUtils.toBytes(size);

        try (InputStream sizeInput = new ByteArrayInputStream(sizeBytes);
             InputStream fileInput = getFileInputStream(file, attributes);
             InputStream sequenceInput = new SequenceInputStream(sizeInput, fileInput)) {

            byte[] hash = HashUtil.getHash(sequenceInput);

            return new FileContentHashIndexExtension(hash);
        }
    }

    private static InputStream getFileInputStream(Path file, BasicFileAttributes attributes) throws IOException {
        if (attributes.isSymbolicLink()) {
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
