package de.silef.service.file.index;

import de.silef.service.file.util.PathVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexPathVisitor extends PathVisitor {

    private List<IndexNode> parentStack;

    private IndexNode root;

    public IndexPathVisitor() throws IOException {
        parentStack = new ArrayList<>();
    }

    public IndexNode getRoot() {
        return root;
    }

    @Override
    public VisitorResult preVisitDirectory(Path path) throws IOException {
        if (!Files.isReadable(path)) {
            return VisitorResult.SKIP;
        }
        parentStack.add(addIndexNode(path));
        return super.preVisitDirectory(path);
    }

    @Override
    public VisitorResult visitFile(Path path) throws IOException {
        if (Files.isReadable(path)) {
            addIndexNode(path);
        }
        return super.visitFile(path);
    }

    @Override
    public VisitorResult postVisitDirectory(Path dir) throws IOException {
        root = parentStack.remove(parentStack.size() - 1);
        return super.postVisitDirectory(dir);
    }


    private IndexNode addIndexNode(Path path) throws IOException {
        if (parentStack.isEmpty()) {
            return IndexNode.createRootFromPath(path);
        }

        IndexNode parent = parentStack.get(parentStack.size() - 1);
        return IndexNode.createFromPath(parent, path);
    }

}
