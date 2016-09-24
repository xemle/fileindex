package de.silef.service.file.util;

/**
 * Created by sebastian on 23.09.16.
 */
public class DataUtils {

    public static byte[] toBytes(long size) {
        byte[] bytes = new byte[8];
        bytes[0] = (byte) ((size >> 56) & 0xff);
        bytes[1] = (byte) ((size >> 48) & 0xff);
        bytes[2] = (byte) ((size >> 40) & 0xff);
        bytes[3] = (byte) ((size >> 32) & 0xff);
        bytes[4] = (byte) ((size >> 24) & 0xff);
        bytes[5] = (byte) ((size >> 16) & 0xff);
        bytes[6] = (byte) ((size >> 8) & 0xff);
        bytes[7] = (byte) ((size >> 0) & 0xff);
        return bytes;
    }

}
