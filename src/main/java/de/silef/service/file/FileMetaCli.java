package de.silef.service.file;

import de.silef.service.file.meta.FileMetaCache;
import de.silef.service.file.meta.FileMetaCacheReader;
import de.silef.service.file.meta.FileMetaCacheWriter;
import de.silef.service.file.meta.FileMetaChanges;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileMetaCli {

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.out.println("FileMetaCli [dir]");
            System.exit(1);
        }
        Path dir = Paths.get(args[0]);
        if (!dir.toFile().isDirectory()) {
            System.out.println("File must be an directory: " + dir);
            System.exit(1);
        }

        System.out.println("Reading tree");
        long t1 = System.currentTimeMillis();
        FileMetaCache cache = new FileMetaCache(dir);
        long t2 = System.currentTimeMillis();
        System.out.println("Reading tree took " + (t2 - t1) + "ms");

        Path cacheFile = dir.resolve(".filecache");
        if (cacheFile.toFile().exists()) {
            System.out.println("Reading cache");

            long t3 = System.currentTimeMillis();
            FileMetaCache old = new FileMetaCacheReader().read(dir, cacheFile, true);
            long t4 = System.currentTimeMillis();

            System.out.println("Reading cache took " + (t4 - t3) + "ms");
            FileMetaChanges changes = cache.getChanges(old);
            if (changes.hasChanges()) {
                System.out.println("Created: " + changes.getCreated());
                System.out.println("Changed: " + changes.getModified());
                System.out.println("Removed: " + changes.getRemoved());
            }
        }

        System.out.println("Writing cache");
        long t5 = System.currentTimeMillis();
        new FileMetaCacheWriter().write(cache, cacheFile);
        long t6 = System.currentTimeMillis();
        System.out.println("Writing cache took " + (t6 - t5) + "ms");
    }

}
