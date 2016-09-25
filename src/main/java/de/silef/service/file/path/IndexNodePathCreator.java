package de.silef.service.file.path;

import de.silef.service.file.node.IndexNode;
import de.silef.service.file.tree.*;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by sebastian on 25.09.16.
 */
public class IndexNodePathCreator {

    private IndexNodePathFactory pathFactory;

    public IndexNodePathCreator(IndexNodePathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }

    public IndexNode create(Path base, CreatePathFilter pathFilter) throws IOException {
        Visitor<Path> resolveLinkVisitor = new ResolveLinkVisitorFilter(base);
        Visitor<Path> filterVisitor = new VisitorFilter<>(pathFilter::isValidPath);
        IndexNodePathVisitor nodeVisitor = new IndexNodePathVisitor(pathFactory);
        VisitorChain<Path> visitorChain = new VisitorChain<>(resolveLinkVisitor, filterVisitor, nodeVisitor);

        Visitor<Path> suppressErrorVisitor = new SuppressErrorPathVisitor<>(visitorChain);
        PathWalker.walk(base, suppressErrorVisitor);

        return nodeVisitor.getRoot();
    }
}
