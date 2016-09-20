package de.silef.service.file.tree;

import java.io.IOException;

/**
 * Created by sebastian on 17.09.16.
 */
public class Visitor<T> {
    public enum VisitorResult {
        CONTINUE,
        /**
         * Skip current node. If returned from preVisitDirectory, postVisitDirectory it not called
         */
        SKIP,
        /**
         * Skip following siblings. The walker should walk depth first. So nested directories are walked first.
         */
        SKIP_SIBLINGS,
        /**
         * Terminate walk immediately. postVisitDirectory is not called
         */
        TERMINATE
    }

    /**
     * Enter current directory
     *
     * @param dir Current directory
     * @return If SKIP is returned, no further action for this subbranch will be taken
     * @throws IOException
     */
    public VisitorResult preVisitDirectory(T dir) throws IOException {
        return VisitorResult.CONTINUE;
    }

    public VisitorResult visitFile(T file) throws IOException {
        return VisitorResult.CONTINUE;
    }

    public VisitorResult postVisitDirectory(T dir) throws IOException {
        return VisitorResult.CONTINUE;
    }

}
