package de.silef.service.file.node;

/**
 * See man 2 stat for description
 *
 * Created by sebastian on 17.09.16.
 */
public enum FileMode {
    FILE      (0100000),
    LINK      (0120000),
    DIRECTORY (0040000),
    OTHER     (0000000);

    int value;

    FileMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    static FileMode create(int value) {
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

    public boolean isDirectory() {
        return value == DIRECTORY.value;
    }

    public boolean sameFileType(FileMode other) {
        return value != OTHER.value && value == other.value;
    }
}
