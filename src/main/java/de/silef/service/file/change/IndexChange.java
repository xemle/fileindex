package de.silef.service.file.change;

import de.silef.service.file.node.IndexNode;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexChange {

    private Path base;

    private List<IndexNodeChange> changes;

    public IndexChange(Path base, List<IndexNodeChange> changes) {
        this.base = base;
        this.changes = changes;
    }

    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    public Path getBase() {
        return base;
    }

    public List<IndexNodeChange> getChanges() {
        return changes;
    }

    public void apply() {
        if (!hasChanges()) {
            return;
        }

        List<IndexNodeChange> sortedChanges = changes.stream()
                .sorted(orderToRemovedModifiedCreated())
                .collect(Collectors.toList());
        for (IndexNodeChange nodeChange : sortedChanges) {
            IndexNode origin = nodeChange.getOrigin();
            IndexNode update = nodeChange.getUpdate();

            if (nodeChange.getChange() == IndexNodeChange.Change.CREATED ) {
                if (!origin.isDirectory()) {
                    throw new IllegalArgumentException("Origin node for created must be a directory");
                }
                origin.addChild(update);
            } else if (nodeChange.getChange() == IndexNodeChange.Change.MODIFIED) {
                origin.setExtensions(update.getExtensions());
            } else if (nodeChange.getChange() == IndexNodeChange.Change.REMOVED) {
                origin.getParent().removeChildByName(origin.getName());
            }
        }
    }

    private Comparator<IndexNodeChange> orderToRemovedModifiedCreated() {
        return (a, b) -> b.getChange().compareTo(a.getChange());
    }

    public List<IndexNodeChange> getCreated() {
        return changes.stream().filter(c -> c.getChange() == IndexNodeChange.Change.CREATED).collect(Collectors.toList());
    }

    public List<IndexNodeChange> getModified() {
        return changes.stream().filter(c -> c.getChange() == IndexNodeChange.Change.MODIFIED).collect(Collectors.toList());
    }

    public List<IndexNodeChange> getRemoved() {
        return changes.stream().filter(c -> c.getChange() == IndexNodeChange.Change.REMOVED).collect(Collectors.toList());
    }

}
