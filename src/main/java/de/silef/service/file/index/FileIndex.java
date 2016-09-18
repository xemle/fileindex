package de.silef.service.file.index;

import de.silef.service.file.hash.FileHash;
import de.silef.service.file.hash.HashUtil;
import de.silef.service.file.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndex {

    private static final Logger LOG = LoggerFactory.getLogger(FileIndex.class);

    private Path base;

    private IndexNode root;

    private Predicate<Path> indexPathFilter;

    private Predicate<IndexNode> hashNodeFilter;

    public FileIndex(Path base) throws IOException {
        this(base, p -> true, n -> true);
    }

    public FileIndex(Path base, Predicate<Path> indexPathFilter, Predicate<IndexNode> hashNodeFilter) throws IOException {
        this(base, build(base, indexPathFilter), indexPathFilter, hashNodeFilter);
    }

    public FileIndex(Path base, IndexNode root) {
        this(base, root, p -> true, n -> true);
    }

    public FileIndex(Path base, IndexNode root, Predicate<Path> indexPathFilter, Predicate<IndexNode> hashNodeFilter) {
        this.base = base;
        this.root = root;
        this.indexPathFilter = indexPathFilter;
        this.hashNodeFilter = hashNodeFilter;
    }

    private static IndexNode build(Path base, Predicate<Path> indexPathFilter) throws IOException {
        IndexPathVisitor cacheVisitor = new IndexPathVisitor();
        PathVisitor realPathVisitor = new RealPathVisitorFilter(base, cacheVisitor);
        PathVisitor filterVisitor = new PathVisitorFilter(indexPathFilter, realPathVisitor);
        PathVisitor suppressErrorVisitor = new SuppressErrorPathVisitor(filterVisitor);

        PathWalker.walk(base, suppressErrorVisitor);

        IndexNode root = cacheVisitor.getRoot();
        calculateRootHash(root);
        return root;
    }

    private static void calculateRootHash(IndexNode root) {
        resetAllDirecoryHashes(root);
        root.getHash();
    }

    private static void resetAllDirecoryHashes(IndexNode root) {
        root.stream()
                .filter(n -> n.getMode() == FileMode.DIRECTORY)
                .forEach(IndexNode::resetHashesToRootNode);
    }

    public void initializeTreeHash() throws IOException {
        IndexNode emptyRoot = IndexNode.createRootFromPath(base);
        IndexChange change = IndexChange.create(base, root, emptyRoot);
        LOG.info("Initialize file index with {} files of {}", getTotalFileCount(), ByteUtil.toHumanSize(getTotalFileSize()));
        updateChanges(change, false);
    }

    public IndexChange getChanges() throws IOException {
        return getChanges(new FileIndex(base, indexPathFilter, hashNodeFilter));
    }

    public long getTotalFileSize() {
        return root.stream().map(IndexNode::getSize).reduce(0L, (a, b) -> a + b);
    }

    public long getTotalFileCount() {
        return root.stream().count();
    }

    public IndexChange getChanges(FileIndex other) {
        return IndexChange.create(base, this.getRoot(), other.getRoot());
    }

    public void update() throws IOException {
        update(false);
    }

    public void update(boolean suppressErrors) throws IOException {
        updateChanges(getChanges(), suppressErrors);
    }

    public void updateChanges(IndexChange change, boolean suppressErrors) throws IOException {
        LOG.debug("Updating index with change: {}", change);
        new IndexUpdater(base, root).update(change, createHashUpdater(), suppressErrors);
    }

    private Consumer<IndexNode> createHashUpdater() {
        return node -> {
                if (!hashNodeFilter.test(node)) {
                    return;
                }
                Path file = base.resolve(node.getRelativePath());
                if (!Files.isRegularFile(file)) {
                    return;
                }
                try {
                    byte[] hash = HashUtil.getHash(file);
                    node.setHash(new FileHash(hash));
                } catch (IOException e) {
                    LOG.warn("Could not update content hash from {}", file);
                }
            };
    }

    public IndexNode getRoot() {
        return root;
    }
}
