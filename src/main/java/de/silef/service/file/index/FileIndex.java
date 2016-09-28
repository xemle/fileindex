package de.silef.service.file.index;

import de.silef.service.file.change.IndexChange;
import de.silef.service.file.change.IndexChangeCreator;
import de.silef.service.file.change.IndexNodeChangeAnalyser;
import de.silef.service.file.extension.BasicFileIndexExtension;
import de.silef.service.file.extension.ExtensionType;
import de.silef.service.file.node.IndexNode;
import de.silef.service.file.node.IndexNodeFactory;
import de.silef.service.file.node.IndexNodeReader;
import de.silef.service.file.node.IndexNodeWriter;
import de.silef.service.file.path.PathInfoFilter;
import de.silef.service.file.path.IndexNodePathCreator;
import de.silef.service.file.path.IndexNodePathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndex {

    private static final Logger LOG = LoggerFactory.getLogger(FileIndex.class);

    private Path base;

    private IndexNode root;

    public FileIndex(Path base, IndexNode root) throws IOException {
        this.base = base;
        this.root = root;
    }

    public static FileIndex create(Path base, IndexNodePathFactory nodePathFactory) throws IOException {
        return create(base, (p) -> true, nodePathFactory);
    }

    public static FileIndex create(Path base, PathInfoFilter pathInfoFilter, IndexNodePathFactory nodePathFactory) throws IOException {
        IndexNodePathCreator pathCreator = new IndexNodePathCreator(nodePathFactory);
        IndexNode root = pathCreator.create(base, pathInfoFilter);

        return new FileIndex(base, root);
    }

    public static FileIndex readFromPath(Path base, Path indexfile, IndexNodeFactory nodeFactory) throws IOException {
        IndexNode root = new IndexNodeReader(nodeFactory).read(indexfile);
        return new FileIndex(base, root);
    }

    public IndexChange getChanges(FileIndex other, IndexNodeChangeAnalyser changeAnalyser) {
        IndexChangeCreator changeCreator = new IndexChangeCreator(changeAnalyser);
        return changeCreator.create(this.getBase(), this.getRoot(), other.getRoot());
    }

    public void applyChanges(IndexChange change) {
        change.apply();
    }

    public void writeToPath(Path indexfile) throws IOException {
        if (!Files.isDirectory(indexfile.getParent())) {
            Files.createDirectories(indexfile.getParent());
        }
        new IndexNodeWriter().write(getRoot(), indexfile);
    }

    public IndexNode getRoot() {
        return root;
    }

    public long getTotalFileSize() {
        return root.stream()
                .filter(n -> n.hasExtensionType(ExtensionType.BASIC_FILE.value))
                .map(n -> ((BasicFileIndexExtension) n.getExtensionByType(ExtensionType.BASIC_FILE.value)).getSize())
                .reduce(0L, (a, b) -> a + b);
    }

    public long getTotalFileCount() {
        return root.stream().count();
    }

    public Path getBase() {
        return base;
    }
}
