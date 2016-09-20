package de.silef.service.file.node;

import de.silef.service.file.tree.Visitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexNodeVisitor extends Visitor<Path> {

    private List<IndexNode> parentStack;

    private Map<Path, List<IndexNode>> pathToChildren = new HashMap<>();

    private IndexNode lastDirNode;

    public IndexNodeVisitor() throws IOException {
        parentStack = new ArrayList<>();
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
            node = IndexNode.createRootFromPath(path);
            pathToChildren.put(path, new ArrayList<>());
        } else {
            IndexNode parent = parentStack.get(parentStack.size() - 1);
            node = IndexNode.createFromPath(parent, path);
            pathToChildren.get(path.getParent()).add(node);
        }

        parentStack.add(node);
        pathToChildren.put(path, new ArrayList<>());
        return super.preVisitDirectory(path);
    }

    @Override
    public VisitorResult visitFile(Path path) throws IOException {
        if (Files.isReadable(path)) {
            IndexNode parent = parentStack.get(parentStack.size() - 1);
            IndexNode child = IndexNode.createFromPath(parent, path);
            pathToChildren.get(path.getParent()).add(child);
        }
        return super.visitFile(path);
    }

    @Override
    public VisitorResult postVisitDirectory(Path dir) throws IOException {
        lastDirNode = parentStack.remove(parentStack.size() - 1);
        List<IndexNode> children = pathToChildren.remove(dir);
        lastDirNode.setChildren(children);
        return super.postVisitDirectory(dir);
    }

}
