package de.silef.service.file;

import de.silef.service.file.index.FileIndex;
import de.silef.service.file.index.FileIndexReader;
import de.silef.service.file.index.FileIndexWriter;
import de.silef.service.file.meta.FileMetaCache;
import de.silef.service.file.meta.FileMetaCacheReader;
import de.silef.service.file.meta.FileMetaCacheWriter;
import de.silef.service.file.meta.FileMetaChanges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndexCli {

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.out.println("FileIndexCli [dir]");
            System.exit(1);
        }
        Path dir = Paths.get(args[0]);
        if (!dir.toFile().isDirectory()) {
            System.out.println("File must be an directory: " + dir);
            System.exit(1);
        }

        Path cacheFile = dir.resolve(".filecache");
        Path indexFile = dir.resolve(".fileindex");

        FileMetaCache cache = new FileMetaCache(dir);
        FileMetaChanges changes;
        if (Files.exists(cacheFile)) {
            System.out.println("Reading file cache");
            FileMetaCache old = new FileMetaCacheReader().read(dir, cacheFile, true);
            changes = cache.getChanges(old);
        } else {
            System.out.println("Creating file cache");
            FileMetaCache empty = new FileMetaCache(dir, new HashMap<>());
            changes = cache.getChanges(empty);
        }

        FileIndex index;
        if (Files.exists(indexFile)) {
            System.out.println("Reading file index");
            index = new FileIndexReader().read(dir, indexFile);
        } else {
            System.out.println("Creating file index");
            index = new FileIndex(dir);
            index.init(cache.getPaths());
        }
        System.out.println("Updating file index");
        index.updateChanges(changes, false);
        if (changes.hasChanges()) {
            new FileMetaCacheWriter().write(cache, cacheFile);
            new FileIndexWriter().write(index, indexFile);
        }
    }
}
