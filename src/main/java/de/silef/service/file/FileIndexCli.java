package de.silef.service.file;

import de.silef.service.file.index.FileIndex;
import de.silef.service.file.index.FileIndexReader;
import de.silef.service.file.index.FileIndexWriter;
import de.silef.service.file.meta.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            FileMetaCache old = new FileMetaCacheReader().read(dir, cacheFile);
            changes = cache.getChanges(old);
        } else {
            System.out.println("Creating file cache");
            FileMetaCache empty = new FileMetaCache(dir, new FileMetaNode(null, dir));
            changes = cache.getChanges(empty);
        }

        FileIndex index = null;
        if (Files.exists(indexFile)) {
            System.out.println("Reading file index");
            try {
                index = new FileIndexReader().read(dir, indexFile, true);
            } catch (IOException e) {
                index = null;
            }
        }
        if (index == null) {
            System.out.println("Creating file index");
            index = new FileIndex(dir);
        }
        System.out.println("Updating file index");
        index.updateChanges(changes, false);
        if (changes.hasChanges()) {
            new FileIndexWriter().write(index, indexFile);
            System.out.println("File index is updated");
            new FileMetaCacheWriter().write(cache, cacheFile);
            System.out.println("File cache is updated");
        }
    }
}
