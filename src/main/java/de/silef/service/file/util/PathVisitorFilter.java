package de.silef.service.file.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * Created by sebastian on 18.09.16.
 */
public class PathVisitorFilter extends PathVisitor {

    private Predicate<Path> filter;

    private PathVisitor delegate;

    public PathVisitorFilter(Predicate<Path> filter, PathVisitor delegate) {
        this.filter = filter;
        this.delegate = delegate;
    }

    @Override
    public VisitorResult preVisitDirectory(Path dir) throws IOException {
        if (!filter.test(dir)) {
            return VisitorResult.SKIP;
        }
        return delegate.preVisitDirectory(dir);
    }

    @Override
    public VisitorResult visitFile(Path file) throws IOException {
        if (!filter.test(file)) {
            return VisitorResult.SKIP;
        }
        return delegate.visitFile(file);
    }

    @Override
    public VisitorResult postVisitDirectory(Path dir) throws IOException {
        return delegate.postVisitDirectory(dir);
    }
}
