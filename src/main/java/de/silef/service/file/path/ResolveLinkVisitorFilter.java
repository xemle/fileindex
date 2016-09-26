package de.silef.service.file.path;

import de.silef.service.file.tree.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Created by sebastian on 17.09.16.
 */
public class ResolveLinkVisitorFilter extends Visitor<PathInfo> {

    private static final Logger LOG = LoggerFactory.getLogger(ResolveLinkVisitorFilter.class);

    private Path base;

    public ResolveLinkVisitorFilter(Path base) throws IOException {
        this.base = base.toRealPath();
    }

    @Override
    public VisitorResult preVisitDirectory(PathInfo dirInfo) throws IOException {
        if (hasSameBasePath(dirInfo)) {
            return super.preVisitDirectory(dirInfo);
        }
        return VisitorResult.SKIP;
    }

    @Override
    public VisitorResult visitFile(PathInfo fileInfo) throws IOException {
        if (hasSameBasePath(fileInfo)) {
            return super.visitFile(fileInfo);
        }
        return VisitorResult.SKIP;
    }

    private boolean hasSameBasePath(PathInfo pathInfo) throws IOException {
        if (pathInfo.isSymbolicLink()) {
            try {
                Path target = Files.readSymbolicLink(pathInfo.getPath());
                Path resolved = pathInfo.getPath().getParent().resolve(target).toAbsolutePath();
                return resolved.startsWith(base);
            } catch (NoSuchFileException e) {
                LOG.warn("Failed to resolve link: {}", pathInfo.getPath());
                return false;
            }
        }
        return true;
    }
}
