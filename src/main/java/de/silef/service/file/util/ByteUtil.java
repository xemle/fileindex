package de.silef.service.file.util;

/**
 * Created by sebastian on 17.09.16.
 */
public class ByteUtil {
    private static String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};

    public static String toHumanSize(long bytes) {
        int unit = 0;
        while (bytes > 4096) {
            bytes = bytes / 1024;
            unit++;
        }
        return String.valueOf(bytes) + units[unit];
    }
}
