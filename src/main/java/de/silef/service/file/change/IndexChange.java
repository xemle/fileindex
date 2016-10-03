package de.silef.service.file.change;

import de.silef.service.file.extension.ExtensionType;
import de.silef.service.file.extension.FileContentHashIndexExtension;
import de.silef.service.file.node.IndexNode;
import de.silef.service.file.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexChange {

    private static final Logger LOG = LoggerFactory.getLogger(IndexChange.class);

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

    public void expandChanges() {
        List<IndexNodeChange> createExpanded = new LinkedList<>();
        List<IndexNodeChange> removeExpanded = new LinkedList<>();
        changes.stream()
                .filter(c -> c.getChange() == IndexNodeChange.Change.CREATED && c.getUpdate().isDirectory())
                .forEach(c -> c.getUpdate().stream()
                        .filter(n -> n != c.getUpdate())
                        .forEach(n -> createExpanded.add(new IndexNodeChange(IndexNodeChange.Change.CREATED, n.getParent(), n))));
        changes.stream()
                .filter(c -> c.getChange() == IndexNodeChange.Change.REMOVED && c.getOrigin().isDirectory())
                .forEach(c -> c.getOrigin().stream()
                        .filter(n -> n != c.getOrigin())
                        .forEach(n -> removeExpanded.add(new IndexNodeChange(IndexNodeChange.Change.REMOVED, n, null))));

        LOG.debug("Expand {} changes with {} creations and {} removals", changes.size(), createExpanded.size(), removeExpanded.size());
        changes.addAll(createExpanded);
        changes.addAll(removeExpanded);
    }

    public void detectMoves() {
        detectMoves(node -> {
            FileContentHashIndexExtension hash = (FileContentHashIndexExtension) node.getExtensionByType(ExtensionType.FILE_HASH.value);
            if (hash != null) {
                return HashUtil.toHex(hash.getData());
            }
            return null;
        });
    }

    private void detectMoves(Function<IndexNode, String> keyGenerator) {
        List<IndexNodeChange> oldChanges = new LinkedList<>();
        List<IndexNodeChange> moves = new LinkedList<>();

        Map<String, List<IndexNodeChange>> keyToCreated = createHashToChanges(getCreated(), keyGenerator);
        for (IndexNodeChange removed : getRemoved()) {
            String key = keyGenerator.apply(removed.getOrigin());
            if (!keyToCreated.containsKey(key)) {
                continue;
            }
            List<IndexNodeChange> created = keyToCreated.get(key);
            IndexNodeChange target = findBestMatch(removed.getOrigin(), created);
            created.remove(target);
            if (created.isEmpty()) {
                keyToCreated.remove(key);
            }
            oldChanges.add(target);
            oldChanges.add(removed);
            moves.add(new IndexNodeChange(IndexNodeChange.Change.MOVED, removed.getOrigin(), target.getUpdate()));
        }

        LOG.debug("Detect {} moves", moves.size());
        changes.removeAll(oldChanges);
        changes.addAll(moves);
    }

    private Map<String, List<IndexNodeChange>> createHashToChanges(List<IndexNodeChange> changes, Function<IndexNode, String> keyGenerator) {
        Map<String, List<IndexNodeChange>> keyToCreated = new HashMap<>(changes.size());
        for (IndexNodeChange change : changes) {
            String key = keyGenerator.apply(change.getUpdate());
            if (key == null) {
                continue;
            }
            if (!keyToCreated.containsKey(key)) {
                keyToCreated.put(key, new LinkedList<>());
            }
            keyToCreated.get(key).add(change);
        }
        return keyToCreated;
    }

    private IndexNodeChange findBestMatch(IndexNode removed, List<IndexNodeChange> indexNodeCreated) {
        if (indexNodeCreated.size() == 1) {
            return indexNodeCreated.get(0);
        }
        Path removedPath = removed.getRelativePath();
        return indexNodeCreated.stream()
                .sorted((a, b) -> {
                    int distanceA = removedPath.compareTo(a.getUpdate().getRelativePath());
                    int distanceB = removedPath.compareTo(b.getUpdate().getRelativePath());
                    return distanceB - distanceA;
                })
                .findFirst().get();
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

    public List<IndexNodeChange> getMoved() {
        return changes.stream().filter(c -> c.getChange() == IndexNodeChange.Change.MOVED).collect(Collectors.toList());
    }

    public List<IndexNodeChange> getRemoved() {
        return changes.stream().filter(c -> c.getChange() == IndexNodeChange.Change.REMOVED).collect(Collectors.toList());
    }

}
