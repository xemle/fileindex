package de.silef.service.file.path;

import de.silef.service.file.node.IndexNode;
import de.silef.service.file.tree.Visitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by sebastian on 17.09.16.
 */
public class PathInfoVisitor extends Visitor<PathInfo> {

    private Stack<IndexNode> parentStack;

    private Map<Path, List<IndexNode>> pathToChildren = new HashMap<>();

    private IndexNode lastDirNode;

    private IndexNodePathFactory nodeFactory;

    public PathInfoVisitor(IndexNodePathFactory nodeFactory) throws IOException {
        this.nodeFactory = nodeFactory;
        parentStack = new Stack<>();
    }

    public IndexNode getRoot() {
        return lastDirNode;
    }

    @Override
    public VisitorResult preVisitDirectory(PathInfo pathInfo) throws IOException {
        if (!Files.isReadable(pathInfo.getPath())) {
            return VisitorResult.SKIP;
        }

        IndexNode node;
        if (parentStack.isEmpty()) {
            node = nodeFactory.createIndexNode(null, pathInfo);
            pathToChildren.put(pathInfo.getPath(), new ArrayList<>());
        } else {
            IndexNode parent = parentStack.peek();
            node = nodeFactory.createIndexNode(parent, pathInfo);
            pathToChildren.get(pathInfo.getPath().getParent()).add(node);
        }
        if (pathInfo.isSymbolicLink()) {
            return VisitorResult.SKIP;
        }

        parentStack.push(node);
        pathToChildren.put(pathInfo.getPath(), new ArrayList<>());
        return super.preVisitDirectory(pathInfo);
    }

    @Override
    public VisitorResult visitFile(PathInfo file) throws IOException {
        if (file.isFile() || file.isSymbolicLink()) {
            IndexNode parent = parentStack.peek();
            IndexNode child = nodeFactory.createIndexNode(parent, file);
            pathToChildren.get(file.getPath().getParent()).add(child);
        }
        return super.visitFile(file);
    }

    @Override
    public VisitorResult postVisitDirectory(PathInfo dir) throws IOException {
        lastDirNode = parentStack.pop();
        List<IndexNode> children = pathToChildren.remove(dir.getPath());
        lastDirNode.setChildren(children);
        return super.postVisitDirectory(dir);
    }

}
