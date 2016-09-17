package de.silef.service.file.meta;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileMeta implements Serializable {

    private String path;

    private boolean isDirectory;
    private boolean isFile;
    private boolean isLink;
    private boolean isOther;

    private long size;

    private long creationTime;
    private long modifiedTime;

    private long inode;

    public FileMeta(Path file, String path) throws IOException {
        if (path == null) {
            throw new NullPointerException("Path must not be null");
        }
        this.path = path;

        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
        isDirectory = attributes.isDirectory();
        isFile = attributes.isRegularFile();
        isLink = attributes.isSymbolicLink();
        isOther = attributes.isOther();

        size = Files.size(file);

        creationTime = attributes.creationTime().toMillis();
        modifiedTime = attributes.lastModifiedTime().toMillis();

        inode = readInode(attributes);
    }

    private long readInode(BasicFileAttributes attributes) {
        Object key = attributes.fileKey();
        if (key != null) {
            String value = key.toString();
            int start = value.indexOf("ino=");
            if (start >= 0) {
                start += 4;
                int end = start;
                while (end < value.length()) {
                    char c = value.charAt(end);
                    if (c < '0' || c > '9') {
                        break;
                    }
                    end++;
                }
                if (start < end) {
                    return Long.parseLong(value.substring(start, end));
                }
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileMeta that = (FileMeta) o;

        if (inode != that.inode) return false;
        if (modifiedTime != that.modifiedTime) return false;
        if (creationTime != that.creationTime) return false;

        if (size != that.size) return false;

        if (isDirectory != that.isDirectory) return false;
        if (isFile != that.isFile) return false;
        if (isLink != that.isLink) return false;
        if (isOther != that.isOther) return false;

        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + (isDirectory ? 1 : 0);
        result = 31 * result + (isFile ? 1 : 0);
        result = 31 * result + (isLink ? 1 : 0);
        result = 31 * result + (isOther ? 1 : 0);
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (creationTime ^ (creationTime >>> 32));
        result = 31 * result + (int) (modifiedTime ^ (modifiedTime >>> 32));
        result = 31 * result + (int) (inode ^ (inode >>> 32));
        return result;
    }

    public String getPath() {
        return path;
    }
}
