package de.silef.service.file;

import de.silef.service.file.index.*;
import de.silef.service.file.util.ByteUtil;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndexCli {

    private static final Logger LOG = LoggerFactory.getLogger(FileIndexCli.class);

    private Path base;

    private Path indexFile;

    public FileIndexCli(Path base, Path indexFile) {
        this.base = base;
        this.indexFile = indexFile;
    }

    public void run() throws IOException {
        LOG.debug("Reading file index data from {}", base.toAbsolutePath());
        FileIndex index = new FileIndex(base);
        LOG.info("Found {} files of {}", index.getTotalFileCount(), ByteUtil.toHumanSize(index.getTotalFileSize()));

        if (Files.exists(indexFile)) {
            LOG.debug("Reading existing file index from {}", indexFile);
            FileIndex old = new FileIndexReader().read(base, indexFile);

            LOG.debug("Calculating file changes");
            IndexChange changes = index.getChanges(old);

            printChange(changes);

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

    private void printChange(IndexChange changes) {
        List<String> lines = new LinkedList<>();

        lines.addAll(createLines("C  ", changes.getCreated()));
        lines.addAll(createLines("M  ", changes.getModified()));
        lines.addAll(createLines("D  ", changes.getRemoved()));

        lines.stream()
                .sorted((a, b) -> a.substring(3).compareTo(b.substring(3)))
                .forEach(System.out::println);
    }

    private List<String> createLines(String prefix, Collection<IndexNode> nodes) {
        return nodes.stream()
                .map(n -> prefix + n.getRelativePath().toString())
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        Options options = new Options();

        options.addOption("d", true, "Target directory to index. Default is current working directory");
        options.addOption("h", false, "Shop this help");
        options.addOption("i", true, "Index file to store. Default is ~/.cache/filecache/<dirname>.index");

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            Path base = null;
            Path indexFile = null;

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("fileindex", options);
                System.exit(0);
            }
            if (cmd.hasOption("d")) {
                base = Paths.get(cmd.getOptionValue("d"));
            } else {
                base = Paths.get(".");
                LOG.debug("Use current working directory to index");
            }

            if (cmd.hasOption("d")) {
                indexFile = Paths.get(cmd.getOptionValue("i"));
            } else {
                String indexName = base.toRealPath().getFileName() + ".index";
                indexFile = Paths.get(System.getProperty("user.home")).resolve(".cache/filecache").resolve(indexName);
                Files.createDirectories(indexFile.getParent());
                LOG.debug("Use default index file: {}", indexFile);
            }

            new FileIndexCli(base, indexFile).run();
        } catch (IOException | ParseException e) {
            LOG.error("Failed to execute", e);
            System.err.println("Failed to execute: " + e.getMessage());
        }
    }
}
