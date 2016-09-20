package de.silef.service.file.util;

import java.io.*;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by sebastian on 17.09.16.
 */
public class HashUtil {

    private static final int BUFFER_SIZE = 1 << 14;

    public static byte[] getHash(Path path) throws IOException {
        try (InputStream input = new FileInputStream(path.toFile())) {
            return getHash(input);
        }
    }

    public static byte[] getHash(byte[] bytes) throws IOException {
        return getHash(bytes, 0, bytes.length);
    }

    public static byte[] getHash(byte[] bytes, int offset, int len) throws IOException {
        try (InputStream input = new ByteArrayInputStream(bytes, offset, len)) {
            return getHash(input);
        }
    }

    public static byte[] getHash(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            return createHash(inputStream, digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Could not create hash due digest initialization error", e);
        }
    }

    private static byte[] createHash(InputStream inputStream, MessageDigest digest) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buf)) > 0) {
            digest.update(buf, 0, read);
        }
        return digest.digest();
    }

    public static String toHex(byte[] hash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
