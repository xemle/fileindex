package de.silef.service.file.path;

import de.silef.service.file.node.IndexNode;
import de.silef.service.file.path.IndexNodePathFactory;
import de.silef.service.file.tree.Visitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexNodePathVisitor extends Visitor<Path> {

    private Stack<IndexNode> parentStack;

    private Map<Path, List<IndexNode>> pathToChildren = new HashMap<>();

    private IndexNode lastDirNode;

    private IndexNodePathFactory nodeFactory;

    public IndexNodePathVisitor(IndexNodePathFactory nodeFactory) throws IOException {
        this.nodeFactory = nodeFactory;
        parentStack = new Stack<>();
    }

    public IndexNode getRoot() {
        return lastDirNode;
    }

    @Override
    public VisitorResult preVisitDirectory(Path path) throws IOException {
        if (!Files.isReadable(path)) {
            return VisitorResult.SKIP;
        }

        IndexNode node;
        if (parentStack.isEmpty()) {
            node = nodeFactory.createFromPath(null, path);
            pathToChildren.put(path, new ArrayList<>());
        } else {
            IndexNode parent = parentStack.peek();
            node = nodeFactory.createFromPath(parent, path);
            pathToChildren.get(path.getParent()).add(node);
        }
        if (Files.isSymbolicLink(path)) {
            return VisitorResult.SKIP;
        }

        parentStack.push(node);
        pathToChildren.put(path, new ArrayList<>());
        return super.preVisitDirectory(path);
    }

    @Override
    public VisitorResult visitFile(Path file) throws IOException {
        if (Files.isReadable(file) || Files.isSymbolicLink(file)) {
            IndexNode parent = parentStack.peek();
            IndexNode child = nodeFactory.createFromPath(parent, file);
            pathToChildren.get(file.getParent()).add(child);
        }
        return super.visitFile(file);
    }

    @Override
    public VisitorResult postVisitDirectory(Path dir) throws IOException {
        lastDirNode = parentStack.pop();
        List<IndexNode> children = pathToChildren.remove(dir);
        lastDirNode.setChildren(children);
        return super.postVisitDirectory(dir);
    }

}
