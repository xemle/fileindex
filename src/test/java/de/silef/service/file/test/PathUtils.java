package de.silef.service.file.test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by sebastian on 17.09.16.
 */
public class PathUtils {

    private static final String OS_NAME = System.getProperty( "os.name" );
    private static final boolean IS_WINDOWS = OS_NAME.contains( "indow" );
    
    public static Path getResourcePath(String resourceName) {
        final String fileString = PathUtils.class.getClassLoader().getResource(resourceName).getFile();
        final String osClearedfileString = IS_WINDOWS ? fileString.substring(1) : fileString;
        return Paths.get(osClearedfileString);
    }

    public static void copy(Path source, Path target) throws IOException {
        if (Files.isDirectory(source)) {
            if (Files.isDirectory(target)) {
                Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                        Path relative = source.relativize(path);
                        Path targetDir = target.resolve(relative);
                        if (!Files.exists(targetDir)) {
                            Files.createDirectory(targetDir);
                        }
                        return super.preVisitDirectory(path, basicFileAttributes);
                    }

                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                        Path relative = source.relativize(path);
                        Path targetFile = target.resolve(relative);
                        Files.copy(path, targetFile);
                        return super.visitFile(path, basicFileAttributes);
                    }
                });
            } else if (!Files.exists(target)) {
                Files.createDirectories(target);
                copy(source, target);
            } else {
                throw new IOException("Target " + target + " is a file of the directory source " + target);
            }
        } else {
            if (Files.isDirectory(target)) {
                Files.copy(source, target);
            } else {
                Files.createDirectories(target.getParent());
                copy(source, target.getParent());
            }
        }
    }

    public static void delete(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        if (Files.isRegularFile(path)) {
            Files.delete(path);
        } else {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    Files.delete(path);
                    return super.visitFile(path, basicFileAttributes);
                }

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                    Files.delete(path);
                    return super.postVisitDirectory(path, e);
                }
            });
        }
    }
}
