package de.silef.service.file.index;

import de.silef.service.file.tree.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * Created by sebastian on 20.09.16.
 */
public class IndexNodeCreator {

    public static IndexNode create(Path base, Predicate<Path> indexPathFilter) throws IOException {
        Visitor<Path> resolveLinkVisitor = new ResolveLinkVisitorFilter(base);
        Visitor<Path> filterVisitor = new VisitorFilter<>(indexPathFilter);
        IndexNodeVisitor nodeVisitor = new IndexNodeVisitor();
        VisitorChain<Path> visitorChain = new VisitorChain<>(resolveLinkVisitor, filterVisitor, nodeVisitor);

        Visitor<Path> suppressErrorVisitor = new SuppressErrorPathVisitor<>(visitorChain);
        PathWalker.walk(base, suppressErrorVisitor);

        IndexNode root = nodeVisitor.getRoot();
        calculateRootHash(root);
        return root;
    }

    private static void calculateRootHash(IndexNode root) {
        resetAllDirectoryHashes(root);
        root.getHash();
    }

    private static void resetAllDirectoryHashes(IndexNode root) {
        root.stream()
                .filter(n -> n.getMode() == FileMode.DIRECTORY)
                .forEach(IndexNode::resetHashesToRootNode);
    }

}
