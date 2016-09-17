package de.silef.service.file.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by sebastian on 17.09.16.
 */
public class RealPathVisitorFilter extends PathVisitor {

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

    private boolean doDelgate(Path path) throws IOException {
        Path realPath = path.toRealPath();
        return realPath.startsWith(base);
    }
}
