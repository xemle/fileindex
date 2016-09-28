package de.silef.service.file;

import de.silef.service.file.change.IndexChange;
import de.silef.service.file.change.IndexNodeChange;
import de.silef.service.file.extension.*;
import de.silef.service.file.index.FileIndex;
import de.silef.service.file.index.StandardFileIndexStrategy;
import de.silef.service.file.node.IndexNode;
import de.silef.service.file.node.IndexNodeFactory;
import de.silef.service.file.node.IndexNodeWalker;
import de.silef.service.file.path.IndexNodePathFactory;
import de.silef.service.file.path.PathInfoFilter;
import de.silef.service.file.tree.Visitor;
import de.silef.service.file.util.ByteUtil;
import de.silef.service.file.util.HashUtil;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static de.silef.service.file.extension.ExtensionType.*;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndexCli {

    private static final Logger LOG = LoggerFactory.getLogger(FileIndexCli.class);

    private static final String DEFAULT_INDEX_DIR = ".cache/fileindex";
    private static final int CHANGE_OUTPUT_LIMIT = 256;

    private CommandLine cmd;

    public FileIndexCli(CommandLine cmd) {
        this.cmd = cmd;
    }

    private void run() throws IOException, java.text.ParseException {
        Path base = getBase();
        Path indexFile = getIndexFile(base);

        StandardFileIndexStrategy indexStrategy = new StandardFileIndexStrategy();

        if (!Files.exists(indexFile)) {
            FileIndex index = buildIndexFromPath(base, indexStrategy);
            calculateHashes(indexFile, index);
            writeIndex(index, indexFile);
            return;
        }

        FileIndex index = readIndex(base, indexFile, indexStrategy);
        FileIndex currentIndex = buildIndexFromPath(base, indexStrategy);

        IndexChange changes = getIndexChanges(indexStrategy, index, currentIndex);

        if (cmd.hasOption('n')) {
            System.exit(0);
        }

        index.applyChanges(changes);
        calculateHashes(indexFile, index);
        writeIndex(index, indexFile);
    }

    private IndexChange getIndexChanges(StandardFileIndexStrategy indexStrategy, FileIndex index, FileIndex currentIndex) {
        IndexChange changes = index.getChanges(currentIndex, indexStrategy);
        printChange(changes);
        return changes;
    }

    private void calculateHashes(Path indexFile, FileIndex index) throws IOException, java.text.ParseException {
        updateContentHash(index, indexFile);
        ensureUniversalHashOfRoot(index);
    }

    private void updateContentHash(FileIndex index, Path indexFile) throws IOException, java.text.ParseException {
        AtomicBoolean done = new AtomicBoolean();
        addShutdownHook(done, () -> {
            ensureUniversalHashOfRoot(index);
            writeIndex(index, indexFile);
            return null;
        });
        calculateFileContentHashes(index);
        done.set(true);
    }

    private void calculateFileContentHashes(FileIndex index) throws IOException, java.text.ParseException {
        long missingHashBytes = getMissingHashBytes(index);
        LOG.info("Initializing file content hashes of {}. This might take some time!", ByteUtil.toHumanSize(missingHashBytes));

        Predicate<IndexNode> hashFileFilter = createHashFileFilter();

        Path base = index.getBase();
        IndexNodeWalker.walk(index.getRoot(), new Visitor<IndexNode>() {

            long bytesHashed = 0;
            long lastLogBytesHashed = 0;
            long logInterval = (1 << 30); // 1GB

            @Override
            public VisitorResult visitFile(IndexNode file) throws IOException {
                if (!requiresFileHash(file) || !hashFileFilter.test(file)) {
                    return VisitorResult.SKIP;
                }
                Path path = base.resolve(file.getRelativePath());
                try {
                    IndexExtension extension = FileContentHashIndexExtension.create(path);
                    file.addExtension(extension);
                    resetUniversalHashToRoot(file.getParent());
                    logProgress(file);
                } catch (IOException | IllegalArgumentException e) {
                    LOG.warn("Could not create content hash of " + path, e);
                }
                return super.visitFile(file);
            }

            private void logProgress(IndexNode file) {
                bytesHashed += getFileSize(file);
                if (lastLogBytesHashed + logInterval < bytesHashed) {
                    LOG.info("Created content hashes of {}", ByteUtil.toHumanSize(bytesHashed));
                    lastLogBytesHashed += logInterval;
                }
            }

            private long getFileSize(IndexNode file) {
                IndexExtension extension = file.getExtensionByType(BASIC_FILE.value);
                if (extension != null && extension instanceof BasicFileIndexExtension) {
                    return ((BasicFileIndexExtension) extension).getSize();
                }
                return 0;
            }

            private boolean requiresFileHash(IndexNode file) {
                return (file.isFile() || file.isLink()) && !file.hasExtensionType(FILE_HASH.value);
            }

            private void resetUniversalHashToRoot(IndexNode dir) {
                if (dir == null) {
                    return;
                }
                if (dir.hasExtensionType(UNIVERSAL_HASH.value)) {
                    dir.removeExtensionType(UNIVERSAL_HASH.value);
                    resetUniversalHashToRoot(dir.getParent());
                }
            }
        });
        LOG.debug("Initialized file content hashes");
    }

    private long getMissingHashBytes(FileIndex index) {
        return index.getRoot()
                    .stream()
                    .filter(IndexNode::isFile)
                    .filter(n -> !n.hasExtensionType(ExtensionType.FILE_HASH.value))
                    .filter(n -> n.hasExtensionType(ExtensionType.BASIC_FILE.value))
                    .map(n -> ((BasicFileIndexExtension) n.getExtensionByType(BASIC_FILE.value)).getSize())
                    .reduce(0L, (a, b) -> a + b);
    }

    private Predicate<IndexNode> createHashFileFilter() throws java.text.ParseException {
        long maxFileSize = cmd.hasOption('M') ? ByteUtil.toByte(cmd.getOptionValue('M')) : 0;
        return n -> {
            BasicFileIndexExtension extension = (BasicFileIndexExtension) n.getExtensionByType(BASIC_FILE.value);
            if (extension == null) {
                return false;
            }
            long size = extension.getSize();
            if (maxFileSize > 0 && size > maxFileSize) {
                LOG.info("File size exceeds hash calculation limit of {}: {} has file {}", ByteUtil.toHumanSize(maxFileSize), ByteUtil.toHumanSize(size), n.getRelativePath());
                return false;
            }
            return true;
        };
    }

    private FileIndex readIndex(Path base, Path indexFile, IndexNodeFactory nodeFactory) throws IOException {
        LOG.debug("Reading existing file index from {}", indexFile);
        FileIndex index = FileIndex.readFromPath(base, indexFile, nodeFactory);
        LOG.debug("Read index with {} files with {}", index.getTotalFileCount(), ByteUtil.toHumanSize(index.getTotalFileSize()));
        return index;
    }

    private void addShutdownHook(AtomicBoolean done, Callable<Void> hook) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    if (!done.get()) {
                        System.out.print("Interrupted. Cleaning up... ");
                        hook.call();
                        System.out.println("Done");
                    }
                } catch (Exception e) {
                    LOG.error("Could not execute shutdown hook");
                }
            };
        });
    }

    private FileIndex buildIndexFromPath(Path base, StandardFileIndexStrategy indexStrategy) throws IOException {
        return buildIndexFromPath(base, indexStrategy, indexStrategy);
    }

    private FileIndex buildIndexFromPath(Path base, PathInfoFilter pathInfoFilter, IndexNodePathFactory nodeFactory) throws IOException {
        LOG.debug("Building file index from path {}", base.toAbsolutePath());
        FileIndex index = FileIndex.create(base, pathInfoFilter, nodeFactory);
        LOG.info("Built index with {} files of {}", index.getTotalFileCount(), ByteUtil.toHumanSize(index.getTotalFileSize()));
        return index;
    }

    private void writeIndex(FileIndex index, Path indexFile) throws IOException {
        LOG.debug("Writing file index data to {} with {} file of {}", indexFile, index.getTotalFileCount(), ByteUtil.toHumanSize(index.getTotalFileSize()));

        Path tmp = null;
        try {
            tmp = indexFile.getParent().resolve(indexFile.getFileName() + ".tmp");
            index.writeToPath(tmp);
            Files.move(tmp, indexFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            if (tmp != null) {
                Files.delete(tmp);
            }
        }
        LOG.info("Written file index data to {}", indexFile);
    }

    private void ensureUniversalHashOfRoot(FileIndex index) throws IOException {
        IndexNode root = index.getRoot();
        if (!root.hasExtensionType(UNIVERSAL_HASH.value)) {
            UniversalHashIndexExtension extension = UniversalHashIndexExtension.create(root);
            LOG.debug("Root hash is {}", HashUtil.toHex(extension.getData()));
            root.addExtension(extension);
        }
    }

    private Path getIndexFile(Path base) throws IOException {
        Path indexDir;
        if (cmd.hasOption("I")) {
            indexDir = Paths.get(cmd.getOptionValue("I"));
        } else {
            indexDir = Paths.get(System.getProperty("user.home")).resolve(DEFAULT_INDEX_DIR);
        }

        Path indexFile;
        if (cmd.hasOption("i")) {
            indexFile = Paths.get(cmd.getOptionValue("i"));
        } else {
            String indexName = base.toRealPath().getFileName() + ".index";
            indexFile = indexDir.resolve(indexName);
            LOG.debug("Use default index file: {}", indexFile);
        }
        Files.createDirectories(indexFile.getParent());
        return indexFile;
    }

    private Path getBase() throws IOException {
        if (cmd.getArgs().length > 0) {
            Path base = Paths.get(cmd.getArgs()[0]);
            if (Files.isSymbolicLink(base)) {
                LOG.info("Resolve absolute path {} from link {}", base.toRealPath(), base);
                base = base.toRealPath();
            }
            return base;
        } else {
            LOG.debug("Use current working directory to index");
            return Paths.get(".");
        }
    }

    private void printChange(IndexChange changes) {
        if (!changes.hasChanges()) {
            System.out.println("-  No changes");
            return;
        }

        if (changes.getChanges().size() > getChangeOutputLimit()) {
            System.out.println("Too many changes: " + changes.getChanges().size() + " modifications. Skip printing. Change it by --output-limit option");
            return;
        }

        changes.getChanges()
                .stream()
                .sorted((a, b) -> a.getRelativePath().compareTo(b.getRelativePath()))
                .map(c -> getChangeChar(c.getChange()) + "  " + c.getRelativePath())
                .forEach(System.out::println);
    }

    private String getChangeChar(IndexNodeChange.Change change) {
        if (change == IndexNodeChange.Change.CREATED) {
            return "C";
        } else if (change == IndexNodeChange.Change.MODIFIED) {
            return "M";
        } else {
            return "R";
        }
    }

    private long getChangeOutputLimit() {
        if (!cmd.hasOption("output-limit")) {
            return CHANGE_OUTPUT_LIMIT;
        }
        try {
            return Long.parseLong(cmd.getOptionValue("output-limit"));
        } catch (NumberFormatException e) {
            System.err.println("Invalid change output limit. Use default " + CHANGE_OUTPUT_LIMIT);
            LOG.warn("Invalid change output limit", e);
            return CHANGE_OUTPUT_LIMIT;
        }
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
        options.addOption("i", true, "Index file path to store. Default is ~/" + DEFAULT_INDEX_DIR + "/<dirname>.index");
        options.addOption("I", true, "Index directory to store file indices. Default is ~/" + DEFAULT_INDEX_DIR);
        options.addOption("q", false, "Quiet mode");
        options.addOption("n", false, "Print changes only. Requires an existing file index");
        options.addOption(Option.builder()
                .longOpt("output-limit")
                .hasArg(true)
                .desc("Limit change output printing. Default is " + CHANGE_OUTPUT_LIMIT)
                .build());
        options.addOption(Option.builder("M")
                .longOpt("verify-max-size")
                .hasArg(true)
                .desc("Limit content integrity verification by file size. Use 0 to disable")
                .build());
        options.addOption(Option.builder()
                .longOpt("start-delay")
                .hasArg(true)
                .desc("Delays the execution by given seconds. Useful for profiling")
                .build());
        return options;
    }

    private static void delayExecution(CommandLine cmd) throws InterruptedException {
        if (cmd.hasOption("start-delay")) {
            int countdown = Integer.parseInt(cmd.getOptionValue("start-delay"));
            while (countdown > 0) {
                System.out.println("Wait " + (countdown--) + " seconds");
                Thread.sleep(1000);
            }
            System.out.println("Starting...");
        }
    }

    public static void main(String[] args) {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                printHelp(options);
            }

            delayExecution(cmd);
            new FileIndexCli(cmd).run();
        } catch (IOException | ParseException | java.text.ParseException | InterruptedException e) {
            LOG.error("Failed to run fileindex", e);
            System.err.println("Failed to run fileindex: " + e.getMessage());
        }
    }
}
