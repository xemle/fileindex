package de.silef.service.file.index;

import de.silef.service.file.meta.FileMetaChanges;
import de.silef.service.file.util.HashUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndex {

    public static int MAGIC_HEADER = 0x08020305;

    private Map<String, byte[]> pathToHash;

    private Path base;

    public FileIndex(Path base) {
        this(base, new HashMap<>());
    }

    public FileIndex(Path base, Map<String, byte[]> pathToHash) {
        this.base = base;
        this.pathToHash = pathToHash;
    }

    public void init(Collection<String> paths) throws IOException {
        updateAll(paths, false);
    }

    public void init(Collection<String> paths, boolean suppressErrors) throws IOException {
        updateAll(paths, suppressErrors);
    }

    public void updateChanges(FileMetaChanges changes, boolean suppressErrors) throws IOException {
        if (changes.hasChanges()) {
            return;
        }
        updateAll(changes.getCreated(), suppressErrors);
        updateAll(changes.getModified(), suppressErrors);
        removeAll(changes.getRemoved());
    }

    private void updateAll(Collection<String> paths) throws IOException {
        updateAll(paths, false);
    }

    private void updateAll(Collection<String> paths, boolean suppressErrors) throws IOException {
        for (String path : paths) {
            Path file = base.resolve(path);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            try {
                byte[] hash = HashUtil.getHash(file);
                pathToHash.put(path, hash);
            } catch (IOException e) {
                if (!suppressErrors) {
                    throw e;
                }
            }
        }
    }

    private void removeAll(Collection<String> paths) {
        for (String path : paths) {
            pathToHash.remove(path);
        }
    }

    Map<String, byte[]> getPathToHash() {
        return pathToHash;
    }
}
