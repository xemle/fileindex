package de.silef.service.file.index;

import de.silef.service.file.util.ByteUtil;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexChange {

    private Path base;

    private Set<IndexNode> created;

    private Set<IndexNode> modified;

    private Set<IndexNode> removed;

    public IndexChange(Path base, Set<IndexNode> created, Set<IndexNode> modified, Set<IndexNode> removed) {
        this.base = base;
        this.created = created;
        this.modified = modified;
        this.removed = removed;
    }

    public static IndexChange create(Path base, IndexNode primaryRoot, IndexNode otherRoot) {
        Map<Path, IndexNode> primary = getFileNodes(primaryRoot);
        Map<Path, IndexNode> other = getFileNodes(otherRoot);

        Set<IndexNode> created = substract(primary, other);
        Set<IndexNode> modified = intersect(primary, other);
        Set<IndexNode> removed = substract(other, primary);

        return new IndexChange(base, created, modified, removed);
    }

    private static Map<Path, IndexNode> getFileNodes(IndexNode root) {
        return root.stream()
                .filter(n -> n.getMode() == FileMode.FILE)
                .collect(Collectors.toMap(IndexNode::getRelativePath, n -> n));
    }

    private static Set<IndexNode> substract(Map<Path, IndexNode> primary, Map<Path, IndexNode> other) {
        Collection<Path> onlyPrimary = new HashSet<>(primary.keySet());
        onlyPrimary.removeAll(other.keySet());

        return onlyPrimary.stream()
                .map(primary::get)
                .collect(Collectors.toSet());
    }

    private static Set<IndexNode> intersect(Map<Path, IndexNode> primary, Map<Path, IndexNode> other) {
        Collection<Path> common = new HashSet<>(primary.keySet());
        common.retainAll(other.keySet());

        return common.stream()
                .filter(p -> !primary.get(p).equals(other.get(p)))
                .map(primary::get)
                .collect(Collectors.toSet());
    }

    public boolean hasChanges() {
        return !modified.isEmpty() || !created.isEmpty() || !removed.isEmpty();
    }

    public Path getBase() {
        return base;
    }

    public Set<IndexNode> getCreated() {
        return created;
    }

    public Set<IndexNode> getModified() {
        return modified;
    }

    public Set<IndexNode> getRemoved() {
        return removed;
    }

    public long getCreatedFileSize() {
        return sumSize(created);
    }

    public long getModifiedFileSize() {
        return sumSize(modified);
    }

    public long getRemovedFileSize() {
        return sumSize(removed);
    }

    private long sumSize(Set<IndexNode> nodes) {
        return nodes.stream().map(IndexNode::getSize).reduce(0L, (a, b) -> a + b);
    }

    @Override
    public String toString() {
        return "Update contains " + created.size() + " created files with " + ByteUtil.toHumanSize(getCreatedFileSize()) + ", " +
         modified.size() + " modified files with " + ByteUtil.toHumanSize(getModifiedFileSize()) + ", " +
         removed.size() + " removed files with " + ByteUtil.toHumanSize(getRemovedFileSize());
    }


}
