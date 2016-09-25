package de.silef.service.file;

import de.silef.service.file.change.IndexChange;
import de.silef.service.file.change.IndexNodeChange;
import de.silef.service.file.extension.BasicFileIndexExtension;
import de.silef.service.file.extension.FileContentHashIndexExtension;
import de.silef.service.file.extension.UniversalHashIndexExtension;
import de.silef.service.file.index.FileIndex;
import de.silef.service.file.path.CreatePathFilter;
import de.silef.service.file.index.StandardFileIndexStrategy;
import de.silef.service.file.node.*;
import de.silef.service.file.path.IndexNodePathFactory;
import de.silef.service.file.tree.Visitor;
import de.silef.service.file.util.ByteUtil;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.silef.service.file.extension.ExtensionType.BASIC_FILE;
import static de.silef.service.file.extension.ExtensionType.FILE_HASH;
import static de.silef.service.file.extension.ExtensionType.UNIVERSAL_HASH;

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

        StandardFileIndexStrategy strategy = new StandardFileIndexStrategy();

        if (!Files.exists(indexFile)) {
            FileIndex index = initializeIndex(base, strategy, strategy);
            updateContentHash(index, indexFile);
            ensureUniveralHashOfRoot(index);
            writeIndex(index, indexFile);
            return;
        }

        FileIndex index = readIndex(base, indexFile, strategy);
        FileIndex currentIndex = initializeIndex(base, strategy, strategy);

        IndexChange changes = index.getChanges(currentIndex, strategy);
        printChange(changes);

        if (cmd.hasOption('n')) {
            System.exit(0);
        }

        index.applyChanges(changes);
        updateContentHash(index, indexFile);
        ensureUniveralHashOfRoot(index);
        writeIndex(index, indexFile);
    }

    private void updateContentHash(FileIndex index, Path indexFile) throws IOException, java.text.ParseException {
        AtomicBoolean done = new AtomicBoolean();
        addShutdownHook(done, () -> {
            ensureUniveralHashOfRoot(index);
            writeIndex(index, indexFile);
            return null;
        });
        calculateFileContentHashes(index);
        done.set(true);
    }

    private void calculateFileContentHashes(FileIndex index) throws IOException, java.text.ParseException {
        LOG.info("Initializing file content hashes. This might take some time!");

        long maxFileSize = cmd.hasOption('M') ? ByteUtil.toByte(cmd.getOptionValue('M')) : 0;
        Predicate<IndexNode> calculateFilter = n -> {
            BasicFileIndexExtension extension = (BasicFileIndexExtension) n.getExtensionByType(BASIC_FILE.value);
            if (extension == null) {
                return false;
            }
            long size = extension.getSize();
            if (size > maxFileSize) {
                LOG.info("File size exceeds hash calculation limit of {}: {} has file {}", ByteUtil.toHumanSize(maxFileSize), ByteUtil.toHumanSize(size), n.getRelativePath());
                return false;
            }
            return true;
        };

        Path base = index.getBase();
        IndexNodeWalker.walk(index.getRoot(), new Visitor<IndexNode>() {

            @Override
            public VisitorResult visitFile(IndexNode file) throws IOException {
                if (calculateFilter.test(file) && !file.hasExtensionType(FILE_HASH.value)) {
                    Path path = base.resolve(file.getRelativePath());
                    file.addExtension(FileContentHashIndexExtension.create(path));
                    resetUniversalHashToRoot(file.getParent());
                }
                return super.visitFile(file);
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

    private FileIndex initializeIndex(Path base, CreatePathFilter pathFilter, IndexNodePathFactory nodeFactory) throws IOException {
        LOG.debug("Initializing file index from {}", base.toAbsolutePath());
        FileIndex index = FileIndex.create(base, pathFilter, nodeFactory);
        LOG.info("Initialed index with {} files", index.getTotalFileCount(), ByteUtil.toHumanSize(index.getTotalFileSize()));
        return index;
    }

    private void writeIndex(FileIndex index, Path indexFile) throws IOException {
        LOG.debug("Writing file index data to {} with {} file of {}", indexFile, index.getTotalFileCount(), ByteUtil.toHumanSize(index.getTotalFileSize()));

        Path tmp = null;
        try {
            tmp = indexFile.getParent().resolve(indexFile.getFileName() + ".tmp");
            new IndexNodeWriter().write(index.getRoot(), tmp);
            Files.move(tmp, indexFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            if (tmp != null) {
                Files.delete(tmp);
            }
        }
        LOG.info("Written file index data to {}", indexFile);
    }

    private void ensureUniveralHashOfRoot(FileIndex index) throws IOException {
        IndexNode root = index.getRoot();
        if (!root.hasExtensionType(UNIVERSAL_HASH.value)) {
            root.addExtension(UniversalHashIndexExtension.create(root));
        }
    }
    private Path getIndexFile(Path base) throws IOException {
        Path indexFile;
        if (cmd.hasOption("d")) {
            indexFile = Paths.get(cmd.getOptionValue("i"));
        } else {
            String indexName = base.toRealPath().getFileName() + ".index2";
            indexFile = Paths.get(System.getProperty("user.home")).resolve(DEFAULT_INDEX_DIR).resolve(indexName);
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
        options.addOption("i", true, "Index file to store. Default is ~/" + DEFAULT_INDEX_DIR + "/<dirname>.index");
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
