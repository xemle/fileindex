package de.silef.service.file.change;

import de.silef.service.file.node.IndexNode;

import java.nio.file.Path;

/**
 * Created by sebastian on 24.09.16.
 */
public class IndexNodeChange {

    public enum Change { SAME, CREATED, MODIFIED, REMOVED}

    private Change change;

    private IndexNode origin;

    private IndexNode update;

    /**
     * @param change Type of change
     * @param origin If change is CREATED, origin is the parent node for the update node
     * @param update If change is REMOVED, update is null
     */
    public IndexNodeChange(Change change, IndexNode origin, IndexNode update) {
        this.change = change;
        this.origin = origin;
        this.update = update;
    }

    public Change getChange() {
        return change;
    }

    public Path getRelativePath() {
        if (change == Change.CREATED) {
            return update.getRelativePath();
        } else {
            return origin.getRelativePath();
        }
    }

    public IndexNode getOrigin() {
        return origin;
    }

    public IndexNode getUpdate() {
        return update;
    }

    @Override
    public String toString() {
        if (change.equals(Change.SAME)) {
            return "= " + origin.getRelativePath();
        } else if (change.equals(Change.CREATED)) {
            return "+ " + update.getRelativePath();
        } else if (change.equals(Change.MODIFIED)) {
            return "! " + origin.getRelativePath();
        } else {
            return "- " + origin.getRelativePath();
        }
    }
}
