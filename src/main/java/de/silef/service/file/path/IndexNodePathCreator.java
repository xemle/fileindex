package de.silef.service.file.path;

import de.silef.service.file.node.IndexNode;
import de.silef.service.file.tree.SuppressErrorPathVisitor;
import de.silef.service.file.tree.Visitor;
import de.silef.service.file.tree.VisitorChain;
import de.silef.service.file.tree.VisitorFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by sebastian on 25.09.16.
 */
public class IndexNodePathCreator {

    private IndexNodePathFactory pathFactory;

    public IndexNodePathCreator(IndexNodePathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }

    public IndexNode create(Path base) throws IOException {
        return create(base, (p, a) -> true);
    }

    public IndexNode create(Path base, CreatePathFilter pathFilter) throws IOException {
        if (!Files.isDirectory(base)) {
            throw new IOException("Base path must be a directory");
        } else if (Files.isSymbolicLink(base)) {
            throw new IOException("Base path must not a symbolic link");
        }
        Visitor<PathInfo> resolveLinkVisitor = new ResolveLinkVisitorFilter(base);
        Visitor<PathInfo> filterVisitor = new VisitorFilter<>(p -> pathFilter.isValidPath(p.getPath(), p.getAttributes()));
        IndexNodePathVisitor nodeVisitor = new IndexNodePathVisitor(pathFactory);
        VisitorChain<PathInfo> visitorChain = new VisitorChain<>(resolveLinkVisitor, filterVisitor, nodeVisitor);

        Visitor<PathInfo> suppressErrorVisitor = new SuppressErrorPathVisitor<>(visitorChain);
        PathInfo pathInfo = PathInfo.create(base);
        PathWalker.walk(pathInfo, suppressErrorVisitor);

        IndexNode root = nodeVisitor.getRoot();
        if (root == null) {
            throw new IOException("Index root node is empty");
        }
        return nodeVisitor.getRoot();
    }
}
