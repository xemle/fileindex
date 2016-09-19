package de.silef.service.file.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * Created by sebastian on 18.09.16.
 */
public class PathVisitorFilter extends PathVisitor {

    private Predicate<Path> filter;

    public PathVisitorFilter(Predicate<Path> filter) {
        this.filter = filter;
    }

    @Override
    public VisitorResult preVisitDirectory(Path dir) throws IOException {
        if (!filter.test(dir)) {
            return VisitorResult.SKIP;
        }
        return super.preVisitDirectory(dir);
    }

    @Override
    public VisitorResult visitFile(Path file) throws IOException {
        if (!filter.test(file)) {
            return VisitorResult.SKIP;
        }
        return super.visitFile(file);
    }

}
