package de.silef.service.file.meta;

import de.silef.service.file.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndex {

    private static final Logger LOG = LoggerFactory.getLogger(FileIndex.class);

    private Path base;

    private IndexNode root;

    public FileIndex(Path base) throws IOException {
        this(base, build(base));
    }

    public FileIndex(Path base, IndexNode root) {
        this.base = base;
        this.root = root;
    }

    private static IndexNode build(Path base) throws IOException {
        CachePathVisitor cacheVisitor = new CachePathVisitor();
        PathVisitor realPathVisitor = new RealPathVisitorFilter(base, cacheVisitor);
        PathVisitor suppressErrorVisitor = new SuppressErrorPathVisitor(realPathVisitor);

        PathWalker.walk(base, suppressErrorVisitor);

        return cacheVisitor.getRoot();
    }

    public Collection<IndexNode> getFileMetaNodes() {
        return flattenMeta().values();
    }

    public Collection<String> getPaths() {
        return flattenMeta().keySet();
    }

    public IndexChanges getChanges() throws IOException {
        return getChanges(new FileIndex(base));
    }

    public IndexChanges getChanges(FileIndex other) {
        Map<String, IndexNode> meta = flattenMeta();
        Map<String, IndexNode> otherMeta = other.flattenMeta();

        Set<String> created = new HashSet<>(meta.keySet());
        created.removeAll(otherMeta.keySet());

        Set<String> removed = new HashSet<>(otherMeta.keySet());
        removed.removeAll(meta.keySet());

        Set<String> common = new HashSet<>(meta.keySet());
        common.retainAll(otherMeta.keySet());

        Set<String> modified = common.stream()
                .filter(path -> !meta.get(path).equals(otherMeta.get(path)))
                .collect(Collectors.toSet());


        if (LOG.isInfoEnabled()) {
            long totalSize = meta.values().stream().map(IndexNode::getSize).reduce(0L, (a, b) -> a + b);
            long createdSize = sumFileSize(created, meta);
            long modifiedSize = sumFileSize(modified, meta);
            long removedSize = sumFileSize(removed, otherMeta);

            LOG.info("File changes of {} files with {}: {} new files with {},  {} modified files with {},  {} deleted files with {}",
                    meta.size(), ByteUtil.toHumanSize(totalSize),
                    created.size(), ByteUtil.toHumanSize(createdSize),
                    modified.size(), ByteUtil.toHumanSize(modifiedSize),
                    removed.size(), ByteUtil.toHumanSize(removedSize));
        }
        return new IndexChanges(base, created, modified, removed);
    }

    private long sumFileSize(Set<String> created, Map<String, IndexNode> meta) {
        return created.stream()
                .map(meta::get)
                .filter(f -> f != null)
                .map(IndexNode::getSize)
                .reduce(0L, (a, b) -> a + b);
    }

    private Map<String, IndexNode> flattenMeta() {
        Map<String, IndexNode> result = new HashMap<>();
        collectMeta(root, result);
        return result;
    }

    private void collectMeta(IndexNode meta, Map<String, IndexNode> result) {
        if (meta.getMode() != FileMode.DIRECTORY) {
            result.put(meta.getRelativePath().toString(), meta);
        }
        for (IndexNode child : meta.getChildren()) {
            collectMeta(child, result);
        }
    }

    public IndexNode getRoot() {
        return root;
    }
}
