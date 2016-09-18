package de.silef.service.file.index;

/**
 * See man 2 stat for description
 *
 * Created by sebastian on 17.09.16.
 */
public enum FileMode {
    DIRECTORY ((byte) 1),
    FILE      ((byte) 2),
    LINK      ((byte) 4),
    OTHER     ((byte) 0);

    byte value;

    FileMode(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static FileMode create(int value) {
        if (value == FILE.value) {
            return FILE;
        } else if (value == DIRECTORY.value) {
            return DIRECTORY;
        } else if (value == LINK.value) {
            return LINK;
        } else {
            return OTHER;
        }
    }
}
