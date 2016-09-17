package de.silef.service.file.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by sebastian on 17.09.16.
 */
public class SuppressErrorPathVisitor extends PathVisitor {

    private PathVisitor delegate;

    public SuppressErrorPathVisitor(PathVisitor delegate) {
        this.delegate = delegate;
    }

    @Override
    public VisitorResult preVisitDirectory(Path dir) throws IOException {
        try {
            return delegate.preVisitDirectory(dir);
        } catch (IOException e) {
            return VisitorResult.SKIP;
        }
    }

    @Override
    public VisitorResult visitFile(Path file) throws IOException {
        try {
            return delegate.visitFile(file);
        } catch (IOException e) {
            return VisitorResult.SKIP;
        }
    }

    @Override
    public VisitorResult postVisitDirectory(Path file) throws IOException {
        try {
            return delegate.postVisitDirectory(file);
        } catch (IOException e) {
            return VisitorResult.SKIP;
        }
    }
}
