package de.silef.service.file.meta;

import de.silef.service.file.util.PathVisitor;
import de.silef.service.file.util.RealPathVisitorFilter;
import de.silef.service.file.util.PathWalker;
import de.silef.service.file.util.SuppressErrorPathVisitor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileMetaCache {
    private Path base;

    private Map<String, FileMeta> cache = new HashMap<>();

    public FileMetaCache(Path base) throws IOException {
        this(base, build(base));
    }

    public FileMetaCache(Path base, Map<String, FileMeta> cache) {
        this.cache = cache;
        this.base = base;
    }

    private static Map<String, FileMeta> build(Path base) throws IOException {
        CachePathVisitor cacheVisitor = new CachePathVisitor(base);
        PathVisitor realPathVisitor = new RealPathVisitorFilter(base, cacheVisitor);
        PathVisitor suppressErrorVisitor = new SuppressErrorPathVisitor(realPathVisitor);

        PathWalker.walk(base, suppressErrorVisitor);

        return cacheVisitor.getCache();
    }

    public Collection<FileMeta> getFileMetaItems() {
        return cache.values();
    }

    public Collection<String> getPaths() {
        return cache.keySet();
    }

    public FileMetaChanges getChanges() throws IOException {
        return getChanges(new FileMetaCache(base));
    }

    public FileMetaChanges getChanges(List<FileMeta> items) {
        Map<String, FileMeta> cache = new HashMap<>();
        for (FileMeta item : items) {
            cache.put(item.getPath(), item);
        }
        return getChanges(new FileMetaCache(base, cache));
    }

    public FileMetaChanges getChanges(FileMetaCache other) {
        Set<String> created = new HashSet<>(cache.keySet());
        created.removeAll(other.cache.keySet());

        Set<String> removed = new HashSet<>(other.cache.keySet());
        removed.removeAll(cache.keySet());

        Set<String> common = new HashSet<>(cache.keySet());
        common.retainAll(other.cache.keySet());

        Set<String> modified = common.stream()
                .filter(path -> !cache.get(path).equals(other.cache.get(path)))
                .collect(Collectors.toSet());

        return new FileMetaChanges(base, created, modified, removed);
    }
}
