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

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndex {

    private static final Logger LOG = LoggerFactory.getLogger(FileIndex.class);

    private Path base;

    private IndexNode root;

    public FileIndex(Path base) throws IOException {
        this(base, build(base));
    }

    public FileIndex(Path base, IndexNode root) {
        this.base = base;
        this.root = root;
    }

    private static IndexNode build(Path base) throws IOException {
        IndexPathVisitor cacheVisitor = new IndexPathVisitor();
        PathVisitor realPathVisitor = new RealPathVisitorFilter(base, cacheVisitor);
        PathVisitor suppressErrorVisitor = new SuppressErrorPathVisitor(realPathVisitor);

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
        return getChanges(new FileIndex(base));
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
