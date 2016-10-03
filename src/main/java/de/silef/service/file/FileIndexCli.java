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
import de.silef.service.file.path.PathInfo;
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
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
            executeCreateIndex(base, indexFile, indexStrategy);
            return;
        }

        FileIndex index = readIndex(base, indexFile, indexStrategy);

        if (cmd.hasOption("u")) {
            executeUpdateIndex(base, indexStrategy, index);
            calculateHashes(indexFile, index);
            writeIndex(index, indexFile);
        }

        executeDeduplication(base, index, indexStrategy);
    }

    private void executeDeduplication(Path base, FileIndex index, StandardFileIndexStrategy indexStrategy) throws IOException {
        if (!cmd.hasOption("deduplicate")) {
            return;
        }

        if (cmd.hasOption("other-dir")) {
            executeDeduplicateWithOtherIndex(base, index, indexStrategy);
            return;
        }

        Map<String, List<IndexNode>> hashToNodes = createHashToNodes(index);

        long hardLinkCount = 0;
        long savedBytes = 0;
        for (Map.Entry<String, List<IndexNode>> entry : hashToNodes.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }
            Iterator<IndexNode> it = entry.getValue().iterator();

            Path existingPath = base.resolve(it.next().getRelativePath());
            PathInfo existingInfo = indexStrategy.createPathInfo(existingPath);

            while (it.hasNext()) {
                Path linkPath = base.resolve(it.next().getRelativePath());
                PathInfo linkInfo = indexStrategy.createPathInfo(linkPath);
                try {
                    if (indexStrategy.createHardLink(linkInfo, existingInfo)) {
                        LOG.trace("Created hard link from {} to {}", linkPath, existingPath);
                        hardLinkCount++;
                        savedBytes += existingInfo.getAttributes().size();
                    }
                } catch (IOException e) {
                    LOG.error("Could not create hard link from {} to {}", linkPath, existingPath, e);
                }
            }
        }
        LOG.info("Created {} duplicate files with hard links. Saved {}", hardLinkCount, ByteUtil.toHumanSize(savedBytes));
    }

    private void executeDeduplicateWithOtherIndex(Path base, FileIndex index, StandardFileIndexStrategy indexStrategy) throws IOException {
        FileIndex otherIndex = readOtherIndex(indexStrategy);
        if (otherIndex == null) {
            return;
        }

        Map<String, List<IndexNode>> hashToNodes = createHashToNodes(index);
        Map<String, List<IndexNode>> otherHashToNodes = createHashToNodes(otherIndex);

        long hardLinkCount = 0;
        long savedBytes = 0;
        for (Map.Entry<String, List<IndexNode>> entry : hashToNodes.entrySet()) {
            List<IndexNode> otherNodes = otherHashToNodes.get(entry.getKey());
            if (otherNodes == null) {
                continue;
            }
            IndexNode targetNode = entry.getValue().get(0);
            Path existing = base.resolve(targetNode.getRelativePath());
            PathInfo existingInfo = indexStrategy.createPathInfo(existing);

            for (IndexNode otherNode : otherNodes) {
                Path link = otherIndex.getBase().resolve(otherNode.getRelativePath());
                PathInfo linkInfo = indexStrategy.createPathInfo(link);
                try {
                    if (indexStrategy.createHardLink(linkInfo, existingInfo)) {
                        LOG.trace("Created hard link from {} to {}", link, existing);
                        hardLinkCount++;
                        savedBytes += existingInfo.getAttributes().size();
                    }
                } catch (IOException e) {
                    LOG.error("could not create hard link of {} -> {}", link, existing, e);
                }
            }
        }
        LOG.info("Deduplicated {} files with hard links. Saved {}", hardLinkCount, ByteUtil.toHumanSize(savedBytes));
    }

    private FileIndex readOtherIndex(StandardFileIndexStrategy indexStrategy) throws IOException {
        Path otherBase = Paths.get(cmd.getOptionValue("other-dir"));
        Path otherIndexFile;
        if (cmd.hasOption("other-index")) {
            otherIndexFile = Paths.get(cmd.getOptionValue("other-index"));
        } else if (cmd.hasOption("I")) {
            String indexName = otherBase.getFileName() + ".index";
            otherIndexFile = Paths.get(cmd.getOptionValue("I")).resolve(indexName);
        } else {
            System.err.println("Missing option --other-index or -I");
            System.exit(1);
            return null;
        }

        if (!Files.isDirectory(otherBase)) {
            System.err.println("Other index directory must be an directory: " + otherBase);
            System.exit(1);
            return null;
        } else if (!Files.isRegularFile(otherIndexFile)) {
            System.err.println("Other index files not found: " + otherIndexFile);
            System.exit(1);
        }

        return readIndex(otherBase, otherIndexFile, indexStrategy);
    }

    private Map<String, List<IndexNode>> createHashToNodes(FileIndex index) {
        LOG.debug("Building map of file hashes");
        List<IndexNode> nodes = index.getRoot().stream()
                .filter(IndexNode::isFile)
                .filter(n -> !n.isLink())
                .filter(n -> n.hasExtensionType(FILE_HASH.value))
                .filter(n -> n.hasExtensionType(BASIC_FILE.value))
                .filter(n -> (( BasicFileIndexExtension) n.getExtensionByType(BASIC_FILE.value)).getSize() > 0)
                .collect(Collectors.toList());
        LOG.debug("Found {} non empty files with content hashes", nodes.size());

        Map<String, List<IndexNode>> hashToNodes = new HashMap<>(nodes.size());
        long duplicates = 0;
        for (IndexNode node : nodes) {
            FileContentHashIndexExtension hashIndexExtension = (FileContentHashIndexExtension) node.getExtensionByType(FILE_HASH.value);
            String hash = HashUtil.toHex(hashIndexExtension.getData());
            if (!hashToNodes.containsKey(hash)) {
                hashToNodes.put(hash, new LinkedList<>());
            } else {
                duplicates++;
            }
            hashToNodes.get(hash).add(node);
        }
        LOG.debug("Found {} duplicates of {} files", duplicates, nodes.size());
        return hashToNodes;
    }

    private void executeCreateIndex(Path base, Path indexFile, StandardFileIndexStrategy indexStrategy) throws IOException, java.text.ParseException {
        if (!cmd.hasOption("c")) {
            System.err.println("Specify option -c to create index");
            System.exit(1);
        }
        FileIndex index = buildIndexFromPath(base, indexStrategy);
        calculateHashes(indexFile, index);
        writeIndex(index, indexFile);
    }

    private void executeUpdateIndex(Path base, StandardFileIndexStrategy indexStrategy, FileIndex index) throws IOException {
        FileIndex currentIndex = buildIndexFromPath(base, indexStrategy);
        IndexChange changes = getIndexChanges(indexStrategy, index, currentIndex);

        if (cmd.hasOption('n')) {
            return;
        }

        index.applyChanges(changes);
        LOG.debug("Applied {} changes to the index", changes.getChanges().size());
    }

    private IndexChange getIndexChanges(StandardFileIndexStrategy indexStrategy, FileIndex index, FileIndex currentIndex) {
        IndexChange changes = index.getChanges(currentIndex, indexStrategy);
        LOG.debug("Update index with {} changes: {} files created, {} files changed, {} files removed", changes.getChanges().size(), changes.getCreated().size(), changes.getModified().size(), changes.getRemoved().size());
        printChange(changes);
        return changes;
    }

    private void calculateHashes(Path indexFile, FileIndex index) throws IOException, java.text.ParseException {
        if (cmd.hasOption("integrity")) {
            updateContentHash(index, indexFile);
            ensureUniversalHashOfRoot(index);
        }
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
        Path base;
        if (cmd.hasOption("d")) {
            base = Paths.get(cmd.getOptionValue("d"));
        } else if (cmd.getArgs().length > 0) {
            base = Paths.get(cmd.getArgs()[0]);
        } else {
            LOG.debug("Use current working directory to index");
            base = Paths.get(".");
        }

        if (Files.isSymbolicLink(base)) {
            LOG.info("Resolve absolute path {} from link {}", base.toRealPath(), base);
            base = base.toRealPath();
        }
        return base;
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

        options.addOption(Option.builder("h")
                .longOpt("help")
                .hasArg(false)
                .desc("Print this help")
                .build());
        options.addOption(Option.builder("i")
                .longOpt("index")
                .hasArg(true)
                .desc("Index file path to store. Default is ~/" + DEFAULT_INDEX_DIR + "/<dirname>.index")
                .build());
        options.addOption(Option.builder("I")
                .longOpt("index-dir")
                .hasArg(true)
                .desc("Index directory to store file indices. Default is ~/" + DEFAULT_INDEX_DIR)
                .build());
        options.addOption(Option.builder("c")
                .longOpt("create")
                .hasArg(false)
                .desc("Create file index from filesystem in not exist")
                .build());
        options.addOption(Option.builder("u")
                .longOpt("update")
                .hasArg(false)
                .desc("Update file index from filesystem")
                .build());
        options.addOption(Option.builder("d")
                .longOpt("dir")
                .hasArg(true)
                .desc("Root directory of file index")
                .build());
        options.addOption(Option.builder("q")
                .longOpt("quiet")
                .hasArg(false)
                .desc("Quiet mode. Do not print output")
                .build());
        options.addOption(Option.builder("n")
                .longOpt("dry-run")
                .hasArg(false)
                .desc("Do not perform any changes")
                .build());
        options.addOption(Option.builder()
                .longOpt("output-limit")
                .hasArg(true)
                .desc("Limit change output printing. Default is " + CHANGE_OUTPUT_LIMIT)
                .build());
        options.addOption(Option.builder()
                .longOpt("integrity-max-size")
                .hasArg(true)
                .desc("Limit content integrity creation by file size. Use 0 to disable")
                .build());
        options.addOption(Option.builder()
                .longOpt("integrity")
                .hasArg(false)
                .desc("Create content hashes")
                .build());
        options.addOption(Option.builder()
                .longOpt("deduplicate")
                .hasArg(false)
                .desc("Deduplicate files via hard links based on the content hashes. If --other-dir is set the deduplication is performed from primary dir to the other dir")
                .build());
        options.addOption(Option.builder()
                .longOpt("other-dir")
                .hasArg(true)
                .desc("Other root directory to create hard links between two indices")
                .build());
        options.addOption(Option.builder()
                .longOpt("other-index")
                .hasArg(true)
                .desc("Other file index to create hard links between two indices")
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
