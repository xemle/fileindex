package de.silef.service.file.meta;

import de.silef.service.file.util.PathVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sebastian on 17.09.16.
 */
public class CachePathVisitor extends PathVisitor {

    private Path base;

    private Map<String, FileMeta> cache = new HashMap<>();

    public CachePathVisitor(Path base) {
        this.base = base;
    }

    public Map<String, FileMeta> getCache() {
        return cache;
    }

    @Override
    public VisitorResult preVisitDirectory(Path path) throws IOException {
        if (!Files.isReadable(path)) {
            return VisitorResult.SKIP;
        }
        addCacheItem(path);
        return super.preVisitDirectory(path);
    }

    @Override
    public VisitorResult visitFile(Path path) throws IOException {
        if (Files.isReadable(path)) {
            addCacheItem(path);
        }
        return super.visitFile(path);
    }

    private void addCacheItem(Path path) throws IOException {
        String relative = base.relativize(path).toString();
        cache.put(relative, new FileMeta(path, relative));
    }

}
