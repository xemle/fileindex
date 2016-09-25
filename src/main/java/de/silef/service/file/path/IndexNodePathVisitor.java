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
public class IndexNodePathVisitor extends Visitor<PathAttribute> {

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
    public VisitorResult preVisitDirectory(PathAttribute path) throws IOException {
        if (!Files.isReadable(path.getPath())) {
            return VisitorResult.SKIP;
        }

        IndexNode node;
        if (parentStack.isEmpty()) {
            node = nodeFactory.createFromPath(null, path.getPath(), path.getAttributes());
            pathToChildren.put(path.getPath(), new ArrayList<>());
        } else {
            IndexNode parent = parentStack.peek();
            node = nodeFactory.createFromPath(parent, path.getPath(), path.getAttributes());
            pathToChildren.get(path.getPath().getParent()).add(node);
        }
        if (path.isSymbolicLink()) {
            return VisitorResult.SKIP;
        }

        parentStack.push(node);
        pathToChildren.put(path.getPath(), new ArrayList<>());
        return super.preVisitDirectory(path);
    }

    @Override
    public VisitorResult visitFile(PathAttribute file) throws IOException {
        if (file.isFile() || file.isSymbolicLink()) {
            IndexNode parent = parentStack.peek();
            IndexNode child = nodeFactory.createFromPath(parent, file.getPath(), file.getAttributes());
            pathToChildren.get(file.getPath().getParent()).add(child);
        }
        return super.visitFile(file);
    }

    @Override
    public VisitorResult postVisitDirectory(PathAttribute dir) throws IOException {
        lastDirNode = parentStack.pop();
        List<IndexNode> children = pathToChildren.remove(dir.getPath());
        lastDirNode.setChildren(children);
        return super.postVisitDirectory(dir);
    }

}
