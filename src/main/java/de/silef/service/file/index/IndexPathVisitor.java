package de.silef.service.file.index;

import de.silef.service.file.util.PathVisitor;

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
public class IndexPathVisitor extends PathVisitor {

    private List<IndexNode> parentStack;

    private Map<IndexNode, List<IndexNode>> parentToChildren = new HashMap<>();

    private IndexNode lastDirNode;

    public IndexPathVisitor() throws IOException {
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
            parentToChildren.put(node, new ArrayList<>());
        } else {
            IndexNode parent = parentStack.get(parentStack.size() - 1);
            node = IndexNode.createFromPath(parent, path);
            parentToChildren.get(parent).add(node);
        }

        parentStack.add(node);
        parentToChildren.put(node, new ArrayList<>());
        return super.preVisitDirectory(path);
    }

    @Override
    public VisitorResult visitFile(Path path) throws IOException {
        if (Files.isReadable(path)) {
            IndexNode parent = parentStack.get(parentStack.size() - 1);
            IndexNode child = IndexNode.createFromPath(parent, path);
            parentToChildren.get(parent).add(child);
        }
        return super.visitFile(path);
    }

    @Override
    public VisitorResult postVisitDirectory(Path dir) throws IOException {
        lastDirNode = parentStack.remove(parentStack.size() - 1);
        List<IndexNode> children = parentToChildren.remove(lastDirNode);
        lastDirNode.setChildren(children);
        return super.postVisitDirectory(dir);
    }

}
