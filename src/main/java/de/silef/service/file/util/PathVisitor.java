package de.silef.service.file.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by sebastian on 17.09.16.
 */
public class PathVisitor {
    public enum VisitorResult { CONTINUE, SKIP };

    public VisitorResult preVisitDirectory(Path dir) throws IOException {
        return VisitorResult.CONTINUE;
    }

    public VisitorResult visitFile(Path file) throws IOException {
        return VisitorResult.CONTINUE;
    }
}
