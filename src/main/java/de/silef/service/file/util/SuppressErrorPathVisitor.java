package de.silef.service.file.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by sebastian on 17.09.16.
 */
public class SuppressErrorPathVisitor extends PathVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(SuppressErrorPathVisitor.class);

    private PathVisitor delegate;

    public SuppressErrorPathVisitor(PathVisitor delegate) {
        this.delegate = delegate;
    }

    @Override
    public VisitorResult preVisitDirectory(Path dir) throws IOException {
        try {
            return delegate.preVisitDirectory(dir);
        } catch (IOException e) {
            LOG.info("Suppress error: {}", e.getMessage(), e);
            return VisitorResult.SKIP;
        }
    }

    @Override
    public VisitorResult visitFile(Path file) throws IOException {
        try {
            return delegate.visitFile(file);
        } catch (IOException e) {
            LOG.info("Suppress error: {}", e.getMessage(), e);
            return VisitorResult.SKIP;
        }
    }

    @Override
    public VisitorResult postVisitDirectory(Path file) throws IOException {
        try {
            return delegate.postVisitDirectory(file);
        } catch (IOException e) {
            LOG.info("Suppress error: {}", e.getMessage(), e);
            return VisitorResult.SKIP;
        }
    }
}
