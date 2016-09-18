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
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndexCli {

    private static final Logger LOG = LoggerFactory.getLogger(FileIndexCli.class);

    private CommandLine cmd;

    public FileIndexCli(CommandLine cmd) {
        this.cmd = cmd;
    }

    private void run() throws IOException, java.text.ParseException {
        Path base = getBase();
        Path indexFile = getIndexFile(base);

        Predicate<Path> pathIndexFilter = p -> true;
        Predicate<IndexNode> hashNodeFilter = getHashNodeFilter();

        FileIndex index = readIndexMetaData(base, pathIndexFilter, hashNodeFilter);

        if (!Files.exists(indexFile)) {
            initializeIndex(index);
            writeIndex(index, indexFile);
            System.out.println("File index successfully created");
        } else {
            IndexChange changes = getIndexChanges(base, pathIndexFilter, hashNodeFilter, indexFile, index);

            if (changes.hasChanges()) {
                updateIndex(index, changes);
                writeIndex(index, indexFile);
                System.exit(1);
            }
        }
    }

    private FileIndex readIndexMetaData(Path base, Predicate<Path> pathIndexFilter, Predicate<IndexNode> hashNodeFilter) throws IOException {
        LOG.debug("Reading file index data from {}", base.toAbsolutePath());
        FileIndex index = new FileIndex(base, pathIndexFilter, hashNodeFilter);
        LOG.info("Found {} files of {}", index.getTotalFileCount(), ByteUtil.toHumanSize(index.getTotalFileSize()));
        return index;
    }

    private Predicate<IndexNode> getHashNodeFilter() throws java.text.ParseException {
        if (!cmd.hasOption('M')) {
            return node -> true;
        }

        long maxSize = ByteUtil.toByte(cmd.getOptionValue('M'));
        if (maxSize == 0) {
            LOG.info("Disable content integrity verification", ByteUtil.toHumanSize(maxSize), maxSize);
            return node -> false;
        }

        LOG.info("Limit content integrity verification to {} ({} bytes)", ByteUtil.toHumanSize(maxSize), maxSize);
        return node -> {
            if (node.getSize() > maxSize) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("File exceeds verification size of {}: {} with {}", ByteUtil.toHumanSize(maxSize), node.getRelativePath(), ByteUtil.toHumanSize(node.getSize()));
                }
                return false;
            }
            return true;
        };
    }

    private void writeIndex(FileIndex index, Path indexFile) throws IOException {
        LOG.debug("Writing file index data to {} with {} file of {}", indexFile, index.getTotalFileCount(), ByteUtil.toHumanSize(index.getTotalFileSize()));
        new IndexNodeWriter().write(index.getRoot(), indexFile);
        LOG.info("Written file index data to {}. The index root hash is {}", indexFile, index.getRoot().getHash());
    }

    private void initializeIndex(FileIndex index) throws IOException {
        LOG.info("Creating file index. This might take some time!");
        index.initializeTreeHash();
        LOG.debug("File index created");
    }

    private void updateIndex(FileIndex index, IndexChange changes) throws IOException {
        LOG.info("Updating file index of {} files with {} by: ", index.getTotalFileCount(), ByteUtil.toHumanSize(index.getTotalFileSize()), changes);
        index.updateChanges(changes, false);
        LOG.debug("Updated file index");
    }

    private IndexChange getIndexChanges(Path base, Predicate<Path> pathIndexFilter, Predicate<IndexNode> hashNodeFilter, Path indexFile, FileIndex index) throws IOException {
        LOG.debug("Reading existing file index from {}", indexFile);
        IndexNode root = new IndexNodeReader().read(base, indexFile);
        FileIndex old = new FileIndex(base, root, pathIndexFilter, hashNodeFilter);

        LOG.debug("Calculating file changes");
        IndexChange changes = index.getChanges(old);

        if (!cmd.hasOption("q")) {
            printChange(changes);
        }
        return changes;
    }

    private Path getIndexFile(Path base) throws IOException {
        Path indexFile;
        if (cmd.hasOption("d")) {
            indexFile = Paths.get(cmd.getOptionValue("i"));
        } else {
            String indexName = base.toRealPath().getFileName() + ".index";
            indexFile = Paths.get(System.getProperty("user.home")).resolve(".cache/filecache").resolve(indexName);
            LOG.debug("Use default index file: {}", indexFile);
        }
        Files.createDirectories(indexFile.getParent());
        return indexFile;
    }

    private Path getBase() {
        if (cmd.getArgs().length > 0) {
            return Paths.get(cmd.getArgs()[0]);
        } else {
            LOG.debug("Use current working directory to index");
            return Paths.get(".");
        }
    }

    private void printChange(IndexChange changes) {
        if (!changes.hasChanges()) {
            System.out.println("-  No changes");
        }
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

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        String header = "\nFollowing options are available:";
        String footer = "\nPlease consult fileindex.log for detailed program information";
        formatter.printHelp("fileindex <options> [path]", header, options, footer);
        System.exit(0);
    }

    private static Options createOptions() {
        Options options = new Options();

        options.addOption("h", false, "Print this help");
        options.addOption("i", true, "Index file to store. Default is ~/.cache/filecache/<dirname>.index");
        options.addOption("q", false, "Quiet mode");
        options.addOption(Option.builder("M")
                .longOpt("verify-max-size")
                .hasArg(true)
                .desc("Limit content integrity verification by file size")
                .build());
        return options;
    }

    public static void main(String[] args) {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                printHelp(options);
            }

            new FileIndexCli(cmd).run();
        } catch (IOException | ParseException | java.text.ParseException e) {
            LOG.error("Failed to run fileindex", e);
            System.err.println("Failed to run fileindex: " + e.getMessage());
        }
    }
}
