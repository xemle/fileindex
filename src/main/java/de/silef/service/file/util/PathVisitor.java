package de.silef.service.file.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by sebastian on 17.09.16.
 */
public class PathVisitor {
    public enum VisitorResult { CONTINUE, SKIP };

    /**
     * Enter current directory
     *
     * @param dir Current directory
     * @return If SKIP is returned, no further action for this subbranch will be taken
     * @throws IOException
     */
    public VisitorResult preVisitDirectory(Path dir) throws IOException {
        return VisitorResult.CONTINUE;
    }

    public VisitorResult visitFile(Path file) throws IOException {
        return VisitorResult.CONTINUE;
    }

    public VisitorResult postVisitDirectory(Path dir) throws IOException {
        return VisitorResult.CONTINUE;
    }

}
