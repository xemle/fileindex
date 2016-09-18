package de.silef.service.file.util;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sebastian on 17.09.16.
 */
public class ByteUtil {
    private static String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};

    private static final Pattern SIZE_PATTERN = Pattern.compile("^(\\d+\\.)?(\\d+)([KkMmGgTtPp][Bb]?|[Bb])?$");

    public static String toHumanSize(long bytes) {
        int unit = 0;
        while (bytes > 4096) {
            bytes = bytes / 1024;
            unit++;
        }
        return String.valueOf(bytes) + units[unit];
    }

    public static long toByte(String size) throws ParseException {
        Matcher matcher = SIZE_PATTERN.matcher(size);
        if (!matcher.matches()) {
            throw new ParseException("Invalid size. Use 1024, 3.5M or 1gb", 0);
        }
        int factor = 1;
        if (matcher.group(3) != null) {
            String unit = matcher.group(3).toUpperCase();
            if (unit.length() == 1 && !unit.equals("B")) {
                unit += "B";
            }
            int i = 0;
            while (!units[i++].equals(unit)) {
                factor *= 1024;
            }
        }
        if (matcher.group(1) != null) {
            double d = Double.parseDouble(matcher.group(1) + matcher.group(2));
            return (long) (d * factor);
        } else {
            long n = Long.parseLong(matcher.group(2));
            return n * factor;
        }
    }
}
