package de.silef.service.file.extension;

import java.util.Arrays;

/**
 * Created by sebastian on 23.09.16.
 */
public class StandardIndexExtension implements IndexExtension {

    private final byte type;

    private final byte[] data;

    public StandardIndexExtension(byte type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }
}
