package de.silef.service.file.meta;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexChanges {

    private Path base;

    private Set<String> created;

    private Set<String> modified;

    private Set<String> removed;

    public IndexChanges(Path base, Set<String> created, Set<String> modified, Set<String> removed) {
        this.created = created;
        this.modified = modified;
        this.removed = removed;
    }

    public boolean hasChanges() {
        return !modified.isEmpty() || !created.isEmpty() || !removed.isEmpty();
    }

    public Path getBase() {
        return base;
    }

    public List<String> getCreated() {
        return created.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> getModified() {
        return modified.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> getRemoved() {
        return removed.stream()
                .sorted()
                .collect(Collectors.toList());
    }

}
