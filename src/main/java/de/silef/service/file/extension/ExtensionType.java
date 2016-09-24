package de.silef.service.file.extension;

/**
 * Created by sebastian on 23.09.16.
 */
public enum ExtensionType {

    BASIC_FILE((byte) 1),
    UNIX_FILE((byte) 2),
    FILE_HASH((byte) 3),
    UNIVERSAL_HASH((byte) 4);

    public byte value;

    ExtensionType(byte value) {
        this.value = value;
    }

}
