package de.silef.service.file.change;

import de.silef.service.file.node.IndexNode;

/**
 * Created by sebastian on 24.09.16.
 */
public class IndexNodeChange {

    public enum Change { SAME, CREATED, MODIFIED, REMOVED}

    private Change change;

    private IndexNode primary;

    private IndexNode other;

    public IndexNodeChange(Change change, IndexNode primary, IndexNode other) {
        this.change = change;
        this.primary = primary;
        this.other = other;
    }

    public Change getChange() {
        return change;
    }

    public IndexNode getPrimary() {
        return primary;
    }

    public IndexNode getOther() {
        return other;
    }

    @Override
    public String toString() {
        if (change.equals(Change.SAME)) {
            return "= " + primary.getRelativePath();
        } else if (change.equals(Change.CREATED)) {
            return "+ " + other.getRelativePath();
        } else if (change.equals(Change.MODIFIED)) {
            return "! " + primary.getRelativePath();
        } else {
            return "- " + primary.getRelativePath();
        }
    }
}
