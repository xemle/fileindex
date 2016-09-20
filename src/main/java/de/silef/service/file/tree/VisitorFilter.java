package de.silef.service.file.tree;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * Created by sebastian on 18.09.16.
 */
public class VisitorFilter<T> extends Visitor<T> {

    private Predicate<T> filter;

    public VisitorFilter(Predicate<T> filter) {
        this.filter = filter;
    }

    @Override
    public VisitorResult preVisitDirectory(T dir) throws IOException {
        if (!filter.test(dir)) {
            return VisitorResult.SKIP;
        }
        return super.preVisitDirectory(dir);
    }

    @Override
    public VisitorResult visitFile(T file) throws IOException {
        if (!filter.test(file)) {
            return VisitorResult.SKIP;
        }
        return super.visitFile(file);
    }

}
