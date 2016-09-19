package de.silef.service.file.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Created by sebastian on 17.09.16.
 */
public class ResolveLinkPathVisitorFilter extends PathVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(ResolveLinkPathVisitorFilter.class);

    private Path base;

    public ResolveLinkPathVisitorFilter(Path base) throws IOException {
        this.base = base.toRealPath();
    }

    @Override
    public VisitorResult preVisitDirectory(Path dir) throws IOException {
        if (hasSameBasePath(dir)) {
            return super.preVisitDirectory(dir);
        }
        return VisitorResult.SKIP;
    }

    @Override
    public VisitorResult visitFile(Path file) throws IOException {
        if (hasSameBasePath(file)) {
            return super.visitFile(file);
        }
        return VisitorResult.SKIP;
    }

    private boolean hasSameBasePath(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) {
            try {
                Path realPath = path.toRealPath();
                return realPath.startsWith(base);
            } catch (NoSuchFileException e) {
                LOG.warn("Failed to resolve link: {}", path);
                return false;
            }
        }
        return true;
    }
}
