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
public class RealPathVisitorFilter extends PathVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(RealPathVisitorFilter.class);

    private Path base;

    private PathVisitor delgate;

    public RealPathVisitorFilter(Path base, PathVisitor delgate) throws IOException {
        this.base = base.toRealPath();
        this.delgate = delgate;
    }

    @Override
    public VisitorResult preVisitDirectory(Path dir) throws IOException {
        if (doDelgate(dir)) {
            return delgate.preVisitDirectory(dir);
        }
        return VisitorResult.SKIP;
    }

    @Override
    public VisitorResult visitFile(Path file) throws IOException {
        if (doDelgate(file)) {
            return delgate.visitFile(file);
        }
        return VisitorResult.SKIP;
    }

    @Override
    public VisitorResult postVisitDirectory(Path file) throws IOException {
        return delgate.postVisitDirectory(file);
    }

    private boolean doDelgate(Path path) throws IOException {
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
