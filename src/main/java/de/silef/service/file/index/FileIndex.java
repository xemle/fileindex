package de.silef.service.file.index;

import de.silef.service.file.change.IndexChange;
import de.silef.service.file.change.IndexNodeChange;
import de.silef.service.file.change.IndexNodeChangeAnalyser;
import de.silef.service.file.change.IndexNodeChangeVisitor;
import de.silef.service.file.extension.BasicFileIndexExtension;
import de.silef.service.file.extension.ExtensionType;
import de.silef.service.file.node.*;
import de.silef.service.file.path.IndexNodePathFactory;
import de.silef.service.file.path.IndexNodeVisitor;
import de.silef.service.file.path.ResolveLinkVisitorFilter;
import de.silef.service.file.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

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
        return create(base, p -> true, nodePathFactory);
    }

    public static FileIndex create(Path base, ImportPathFilter pathFilter, IndexNodePathFactory nodePathFactory) throws IOException {
        Visitor<Path> resolveLinkVisitor = new ResolveLinkVisitorFilter(base);
        Visitor<Path> filterVisitor = new VisitorFilter<>(pathFilter::importPath);
        IndexNodeVisitor nodeVisitor = new IndexNodeVisitor(nodePathFactory);
        VisitorChain<Path> visitorChain = new VisitorChain<>(resolveLinkVisitor, filterVisitor, nodeVisitor);

        Visitor<Path> suppressErrorVisitor = new SuppressErrorPathVisitor<>(visitorChain);
        PathWalker.walk(base, suppressErrorVisitor);

        return new FileIndex(base, nodeVisitor.getRoot());
    }

    public static FileIndex readFromPath(Path base, Path indexfile, IndexNodeFactory nodeFactory) throws IOException {
        IndexNode root = new IndexNodeReader(nodeFactory).read(base, indexfile);
        return new FileIndex(base, root);
    }

    public IndexChange getChanges(FileIndex other, IndexNodeChangeAnalyser changePredicate) {
        IndexNodeChangeVisitor visitor = new IndexNodeChangeVisitor(root, changePredicate);
        IndexNodeWalker.walk(other.getRoot(), visitor);
        return new IndexChange(base, visitor.getChanges());
    }

    public void applyChanges(IndexChange change) {
        if (!change.hasChanges()) {
            return;
        }

        List<IndexNodeChange> changes = change.getChanges().stream()
                .sorted((a, b) -> b.getChange().compareTo(a.getChange()))
                .collect(Collectors.toList());
        for (IndexNodeChange nodeChange : changes) {
            IndexNode primaryNode = nodeChange.getPrimary();
            IndexNode otherNode = nodeChange.getOther();

            if (nodeChange.getChange() == IndexNodeChange.Change.CREATED) {
                if (otherNode.isDirectory()) {
                    primaryNode.addChild(otherNode);
                } else {
                    primaryNode.getParent().addChild(otherNode);
                }
            } else if (nodeChange.getChange() == IndexNodeChange.Change.MODIFIED) {
                primaryNode.getParent().addChild(otherNode);
            } else if (nodeChange.getChange() == IndexNodeChange.Change.REMOVED) {
                primaryNode.getParent().removeChildByName(otherNode.getName());
            }
        }
    }

    public void writeToPath(Path indexfile) throws IOException {
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
