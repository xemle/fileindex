package de.silef.service.file.path;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Bundle path and file attributes to one object to reduce file attribute readings while
 * creating a file index from path
 */
public class PathInfo {

    private Path path;

    private BasicFileAttributes attributes;

    private String fileNameCache;

    private PathInfo(Path path, BasicFileAttributes attributes) {
        this.path = path;
        this.attributes = attributes;
    }

    public static PathInfo create(Path path)  {
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            return new PathInfo(path, attributes);
        } catch (IOException e) {
            throw new RuntimeException("Could not read attributes from path " + path, e);
        }
    }

    public Path getPath() {
        return path;
    }

    public BasicFileAttributes getAttributes() {
        return attributes;
    }

    public boolean isDirectory() {
        return attributes.isDirectory();
    }

    public boolean isFile() {
        return attributes.isRegularFile();
    }

    public boolean isSymbolicLink() {
        return attributes.isSymbolicLink();
    }

    public String getFileName() {
        if (fileNameCache == null) {
            fileNameCache = path.getFileName().toString();
        }
        return fileNameCache;
    }
}
