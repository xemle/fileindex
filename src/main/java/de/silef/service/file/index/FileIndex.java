package de.silef.service.file.index;

import de.silef.service.file.hash.FileHash;
import de.silef.service.file.hash.HashUtil;
import de.silef.service.file.node.IndexNode;
import de.silef.service.file.node.IndexNodeCreator;
import de.silef.service.file.util.ByteUtil;
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
        this(base, IndexNodeCreator.create(base, indexPathFilter), indexPathFilter, hashNodeFilter);
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

    public void initializeTreeHash() throws IOException {
        IndexNode emptyRoot = IndexNode.createRootFromPath(base);
        IndexChange change = IndexChange.create(base, root, emptyRoot);
        LOG.info("Initialize file index with {} files of {}", getTotalFileCount(), ByteUtil.toHumanSize(getTotalFileSize()));
        updateChanges(change, false);
    }

    public IndexChange getChanges() throws IOException {
        FileIndex current = new FileIndex(base, indexPathFilter, hashNodeFilter);
        return current.getChanges(this);
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
                if (!Files.isSymbolicLink(file) && !Files.isRegularFile(file)) {
                    return;
                }
                try {
                    byte[] hash;
                    if (Files.isRegularFile(file)) {
                        hash = HashUtil.getHash(file);
                    } else {
                        Path link = Files.readSymbolicLink(file);
                        hash = HashUtil.getHash(link.toString().getBytes());
                    }
                    node.setHash(new FileHash(hash));
                } catch (IOException e) {
                    LOG.warn("Could not update content hash from {}", file);
                }
            };
    }

    public IndexNode getRoot() {
        return root;
    }

    public long getTotalFileSize() {
        return root.stream().map(IndexNode::getSize).reduce(0L, (a, b) -> a + b);
    }

    public long getTotalFileCount() {
        return root.stream().count();
    }

    public Path getBase() {
        return base;
    }
}
