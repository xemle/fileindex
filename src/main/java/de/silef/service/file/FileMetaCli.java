package de.silef.service.file;

import de.silef.service.file.meta.FileMetaCache;
import de.silef.service.file.meta.FileMetaChanges;
import de.silef.service.file.meta.FileMeta;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.*;

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

        System.out.println("Reading cache");
        Path cacheFile = dir.resolve(".filecache");
        long t3 = System.currentTimeMillis();
        List<FileMeta> items = readFileMetaCache(cacheFile);
        long t4 = System.currentTimeMillis();
        System.out.println("Reading cache took " + (t4 - t3) + "ms");
        if (items != null) {
            FileMetaChanges changes = cache.getChanges(items);
            if (changes.hasChanges()) {
                System.out.println("Created: " + changes.getCreated());
                System.out.println("Changed: " + changes.getModified());
                System.out.println("Removed: " + changes.getRemoved());
            }
        }

        System.out.println("Writing cache");
        long t5 = System.currentTimeMillis();
        writeFileCache(cache, cacheFile);
        long t6 = System.currentTimeMillis();
        System.out.println("Writing cache took " + (t6 - t5) + "ms");
    }

    private static void writeFileCache(FileMetaCache cache, Path cacheFile) throws IOException {
        try (OutputStream output = new FileOutputStream(cacheFile.toFile());
             DeflaterOutputStream deflaterOutput = new DeflaterOutputStream(output);
             BufferedOutputStream bufferedOutput = new BufferedOutputStream(deflaterOutput);
             ObjectOutputStream objectOutput = new ObjectOutputStream(bufferedOutput)) {
            List<FileMeta> items = new ArrayList<>(cache.getFileMetaItems());
            objectOutput.writeObject(items);
        }
    }

    private static List<FileMeta> readFileMetaCache(Path cacheFile) {
        if (!cacheFile.toFile().canRead()) {
            return null;
        }
        try (InputStream input = new FileInputStream(cacheFile.toFile());
             InflaterInputStream inflaterInput = new InflaterInputStream(input);
             BufferedInputStream bufferedInput = new BufferedInputStream(inflaterInput);
             ObjectInputStream objectInput = new ObjectInputStream(bufferedInput)) {
            Object object = objectInput.readObject();
            if (object instanceof Collection) {
                return (List<FileMeta>) object;
            }
            return null;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            return null;
        }
    }
}
