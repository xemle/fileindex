package de.silef.service.file.tree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by sebastian on 17.09.16.
 */
public class SuppressErrorPathVisitor<T> extends Visitor<T> {

    private static final Logger LOG = LoggerFactory.getLogger(SuppressErrorPathVisitor.class);

    private Visitor<T> delegate;

    public SuppressErrorPathVisitor(Visitor<T> delegate) { this.delegate = delegate;
    }

    @Override
    public VisitorResult preVisitDirectory(T path) throws IOException {
        try {
            return delegate.preVisitDirectory(path);
        } catch (IOException e) {
            LOG.info("Suppress error: {}", e.getMessage(), e);
            return VisitorResult.SKIP;
        }
    }

    @Override
    public VisitorResult visitFile(T file) throws IOException {
        try {
            return delegate.visitFile(file);
        } catch (IOException e) {
            LOG.info("Suppress error: {}", e.getMessage(), e);
            return VisitorResult.SKIP;
        }
    }

    @Override
    public VisitorResult postVisitDirectory(T file) throws IOException {
        try {
            return delegate.postVisitDirectory(file);
        } catch (IOException e) {
            LOG.info("Suppress error: {}", e.getMessage(), e);
            return VisitorResult.SKIP;
        }
    }
}
