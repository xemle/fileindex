package de.silef.service.file;

import de.silef.service.file.meta.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndexCli {

    private static final Logger LOG = LoggerFactory.getLogger(FileIndexCli.class);

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

        FileIndex cache = new FileIndex(dir);
        IndexChanges changes;
        if (Files.exists(cacheFile)) {
            System.out.println("Reading file cache");
            FileIndex old = new FileIndexReader().read(dir, cacheFile);
            changes = cache.getChanges(old);
        } else {
            System.out.println("Creating file cache");
            FileIndex empty = new FileIndex(dir, IndexNode.createRootFromPath(dir));
            changes = cache.getChanges(empty);
        }

        de.silef.service.file.index.FileIndex index = null;
        if (Files.exists(indexFile)) {
            System.out.println("Reading file index");
            try {
                index = new de.silef.service.file.index.FileIndexReader().read(dir, indexFile);
            } catch (IOException e) {
                index = null;
                LOG.warn("Could not read file index", e);
            }
        }
        if (index == null) {
            System.out.println("Creating file index");
            index = new de.silef.service.file.index.FileIndex(dir);
        }
        System.out.println("Updating file index");
        index.updateChanges(changes, false);
        if (changes.hasChanges()) {
            new de.silef.service.file.index.FileIndexWriter().write(index, indexFile);
            System.out.println("File index is updated");
            new FileIndexWriter().write(cache, cacheFile);
            System.out.println("File cache is updated");
        }
    }
}
