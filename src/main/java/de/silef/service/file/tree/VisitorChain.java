package de.silef.service.file.tree;

import java.io.IOException;

/**
 * For preVisitDirectory and visitFile call visitors in forward direction. On postVisitDirectory
 * call visitors in reverse order
 *
 * Created by sebastian on 19.09.16.
 */
public class VisitorChain<T> extends Visitor<T> {

    private Visitor<T>[] visitors;

    @SafeVarargs
    public VisitorChain(Visitor<T>... visitors) {
        this.visitors = visitors;
    }

    @Override
    public VisitorResult preVisitDirectory(T dir) throws IOException {
        VisitorResult result = VisitorResult.CONTINUE;
        for (Visitor<T> visitor : visitors) {
            result = visitor.preVisitDirectory(dir);
            if (result != VisitorResult.CONTINUE) {
                return result;
            }
        }
        return result;
    }

    @Override
    public VisitorResult visitFile(T file) throws IOException {
        VisitorResult result = VisitorResult.CONTINUE;
        for (Visitor<T> visitor : visitors) {
            result = visitor.visitFile(file);
            if (result != VisitorResult.CONTINUE) {
                return result;
            }
        }
        return result;
    }

    @Override
    public VisitorResult postVisitDirectory(T dir) throws IOException {
        VisitorResult result = VisitorResult.CONTINUE;
        for (int i = visitors.length - 1; i >= 0; i--) {
            Visitor<T> visitor = visitors[i];
            result = visitor.postVisitDirectory(dir);
            if (result != VisitorResult.CONTINUE) {
                return result;
            }
        }
        return result;
    }
}
