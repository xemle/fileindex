package de.silef.service.file.path;

import de.silef.service.file.node.IndexNode;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by sebastian on 24.09.16.
 */
public interface IndexNodePathFactory {
    IndexNode createFromPath(IndexNode parent, Path path, BasicFileAttributes attributes) throws IOException;

}