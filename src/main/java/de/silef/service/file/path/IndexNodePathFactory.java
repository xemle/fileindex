package de.silef.service.file.path;

import de.silef.service.file.node.IndexNode;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by sebastian on 24.09.16.
 */
public interface IndexNodePathFactory {

    PathInfo createPathInfo(Path path);

    IndexNode createIndexNode(IndexNode parent, PathInfo pathInfo) throws IOException;

    boolean createHardLink(PathInfo link, PathInfo existing) throws IOException;

}
