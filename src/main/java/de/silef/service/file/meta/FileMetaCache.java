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
public class FileMetaCache {

    private static final Logger LOG = LoggerFactory.getLogger(FileMetaCache.class);

    private Path base;

    private FileMeta root;

    public FileMetaCache(Path base) throws IOException {
        this(base, build(base));
    }

    public FileMetaCache(Path base, FileMeta root) {
        this.base = base;
        this.root = root;
    }

    private static FileMeta build(Path base) throws IOException {
        CachePathVisitor cacheVisitor = new CachePathVisitor();
        PathVisitor realPathVisitor = new RealPathVisitorFilter(base, cacheVisitor);
        PathVisitor suppressErrorVisitor = new SuppressErrorPathVisitor(realPathVisitor);

        PathWalker.walk(base, suppressErrorVisitor);

        return cacheVisitor.getRoot();
    }

    public Collection<FileMeta> getFileMetaItems() {
        return flattenMeta().values();
    }

    public Collection<String> getPaths() {
        return flattenMeta().keySet();
    }

    public FileMetaChanges getChanges() throws IOException {
        return getChanges(new FileMetaCache(base));
    }

    public FileMetaChanges getChanges(FileMetaCache other) {
        Map<String, FileMeta> meta = flattenMeta();
        Map<String, FileMeta> otherMeta = other.flattenMeta();

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
            long createdSize = sumFileSize(created, meta);
            long modifiedSize = sumFileSize(modified, meta);
            long removedSize = sumFileSize(removed, otherMeta);

            LOG.info("File changes: {} new files with {},  {} modified files with {},  {} deleted files with {}",
                    created.size(), ByteUtil.toHumanSize(createdSize),
                    modified.size(), ByteUtil.toHumanSize(modifiedSize),
                    removed.size(), ByteUtil.toHumanSize(removedSize));
        }
        return new FileMetaChanges(base, created, modified, removed);
    }

    private long sumFileSize(Set<String> created, Map<String, FileMeta> meta) {
        return created.stream()
                .map(meta::get)
                .filter(f -> f != null)
                .map(FileMeta::getSize)
                .reduce(0L, (a, b) -> a + b);
    }

    private Map<String, FileMeta> flattenMeta() {
        Map<String, FileMeta> result = new HashMap<>();
        collectMeta(root, result);
        return result;
    }

    private void collectMeta(FileMeta meta, Map<String, FileMeta> result) {
        if (meta.getMode() != FileMode.DIRECTORY) {
            result.put(meta.getRelativePath().toString(), meta);
        }
        for (FileMeta child : meta.getChildren()) {
            collectMeta(child, result);
        }
    }

    public FileMeta getRoot() {
        return root;
    }
}
