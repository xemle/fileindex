package de.silef.service.file.node;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by sebastian on 23.09.16.
 */
public class IndexNodeType {

    public static final IndexNodeType DIRECTORY = new IndexNodeType(0x01);
    public static final IndexNodeType FILE = new IndexNodeType(0x02);
    public static final IndexNodeType SYMLINK = new IndexNodeType(0x04);
    public static final IndexNodeType MASK = new IndexNodeType(0x07);

    private final byte value;

    private IndexNodeType(byte value) {
        this.value = value;
    }

    private IndexNodeType(int value) {
        this.value = (byte) (value & 0xff);
    }

    public static IndexNodeType create(byte value) {
        if ((value & FILE.value) > 0 && (value & DIRECTORY.value) > 0) {
            throw new IllegalArgumentException("Invalid type value. Type must be either file or directory but not both");
        }
        return new IndexNodeType(value & MASK.value);
    }

    public static IndexNodeType create(Path file) {
        boolean isDirectory = Files.isDirectory(file);
        boolean isFile = Files.isRegularFile(file);
        boolean isLink = Files.isSymbolicLink(file);

        int value = 0;
        if (isDirectory) {
            value |= DIRECTORY.value;
        } else if (isFile) {
            value |= FILE.value;
        }
        if (isLink) {
            value |= SYMLINK.value;
        }
        return new IndexNodeType(value & MASK.value);
    }

    public byte getValue() {
        return value;
    }

    public byte getByte() {
        return getValue();
    }

    public boolean isDirectory() {
        return (value & DIRECTORY.value) > 0;
    }

    public boolean isFile() {
        return (value & FILE.value) > 0;
    }

    public boolean isLink() {
        return (value & SYMLINK.value) > 0;
    }

    public boolean isOther() {
        return (value & MASK.value) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexNodeType nodeType = (IndexNodeType) o;

        return value == nodeType.value;

    }

    @Override
    public int hashCode() {
        return (int) value;
    }

    @Override
    public String toString() {
        String result = "";
        if (isDirectory()) {
            result += "DIR";
        } else if (isFile()) {
            result += "FILE";
        } else {
            result += "OTHER";
        }
        if (isLink()) {
            result += "(SYMLINK)";
        }
        return result;
    }
}
