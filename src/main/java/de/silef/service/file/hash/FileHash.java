package de.silef.service.file.hash;

import java.util.Arrays;

/**
 * Created by sebastian on 18.09.16.
 */
public class FileHash {

    public static final int LENGTH = 20;

    public static final FileHash ZERO = new FileHash(new byte[20]);

    private byte[] bytes;

    public FileHash(byte[] bytes) {
        assert bytes != null : "Hash bytes must not be null";
        assert bytes.length == LENGTH : "Hash bytes must have the length of " + LENGTH;

        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileHash fileHash = (FileHash) o;

        return Arrays.equals(bytes, fileHash.bytes);

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return HashUtil.toHex(bytes);
    }

    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }
}
