package de.silef.service.file.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * For preVisitDirectory and visitFile call visitors in forward direction. On postVisitDirectory
 * call visitors in reverse order
 *
 * Created by sebastian on 19.09.16.
 */
public class PathVisitorChain extends PathVisitor {

    private PathVisitor[] visitors;

    public PathVisitorChain(PathVisitor... visitors) {
        this.visitors = visitors;
    }

    @Override
    public VisitorResult preVisitDirectory(Path dir) throws IOException {
        VisitorResult result = VisitorResult.CONTINUE;
        for (PathVisitor visitor : visitors) {
            result = visitor.preVisitDirectory(dir);
            if (result != VisitorResult.CONTINUE) {
                return result;
            }
        }
        return result;
    }

    @Override
    public VisitorResult visitFile(Path file) throws IOException {
        VisitorResult result = VisitorResult.CONTINUE;
        for (PathVisitor visitor : visitors) {
            result = visitor.visitFile(file);
            if (result != VisitorResult.CONTINUE) {
                return result;
            }
        }
        return result;
    }

    @Override
    public VisitorResult postVisitDirectory(Path dir) throws IOException {
        VisitorResult result = VisitorResult.CONTINUE;
        for (int i = visitors.length - 1; i >= 0; i--) {
            PathVisitor visitor = visitors[i];
            result = visitor.postVisitDirectory(dir);
            if (result != VisitorResult.CONTINUE) {
                return result;
            }
        }
        return result;
    }
}
