package de.silef.service.file;

import de.silef.service.file.index.FileIndex;
import de.silef.service.file.index.FileIndexReader;
import de.silef.service.file.index.FileIndexWriter;
import de.silef.service.file.index.IndexChange;
import de.silef.service.file.util.ByteUtil;
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
            LOG.error("FileIndexCli [dir]");
            System.exit(1);
        }
        Path dir = Paths.get(args[0]);
        if (!dir.toFile().isDirectory()) {
            LOG.error("Path must be an directory: " + dir);
            System.exit(1);
        }

        Path indexFile = dir.resolve(".fileindex");

        LOG.debug("Reading file index data from {}", dir.toAbsolutePath());
        FileIndex index = new FileIndex(dir);
        LOG.info("Found {} files of {}", index.getTotalFileCount(), ByteUtil.toHumanSize(index.getTotalFileSize()));

        if (Files.exists(indexFile)) {
            LOG.debug("Reading existing file index from {}", indexFile);
            FileIndex old = new FileIndexReader().read(dir, indexFile);

            LOG.debug("Calculating file changes");
            IndexChange changes = index.getChanges(old);

            LOG.info("Updating file index of {} files with {} by: ", index.getTotalFileCount(), ByteUtil.toHumanSize(index.getTotalFileSize()), changes);
            index.updateChanges(changes, false);
            LOG.debug("Updated file index");
        } else {
            LOG.info("Creating file index. This might take some time!");
            index.initializeTreeHash();
            LOG.debug("File index created");
        }

        LOG.debug("Writing file index data to {} with {} file of {}", indexFile, index.getTotalFileCount(), ByteUtil.toHumanSize(index.getTotalFileSize()));
        new FileIndexWriter().write(index, indexFile);
        LOG.info("Written file index data to {}. The index root hash is {}", indexFile, index.getRoot().getHash());

    }
}
