package de.silef.service.file.change;

import java.nio.file.Path;
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
