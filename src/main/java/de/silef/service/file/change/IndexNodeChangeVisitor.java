package de.silef.service.file.change;

import de.silef.service.file.node.IndexNode;
import de.silef.service.file.tree.Visitor;

import java.io.IOException;
import java.util.*;


/**
 * Created by sebastian on 24.09.16.
 */
public class IndexNodeChangeVisitor extends Visitor<IndexNode> {

    private IndexNode currentRoot;

    private Stack<IndexNode> originStack = new Stack<>();
    private Stack<IndexNode> currentStack = new Stack<>();

    private Map<IndexNode, Set<String>> currentChildNamesVisited = new HashMap<>();

    private List<IndexNodeChange> changes = new LinkedList<>();

    private IndexNodeChangeFactory changeFactory;

    public IndexNodeChangeVisitor(IndexNode currentRoot, IndexNodeChangeFactory changeFactory) {
        this.currentRoot = currentRoot;
        this.changeFactory = changeFactory;
    }

    public List<IndexNodeChange> getChanges() {
        return changes;
    }

    @Override
    public VisitorResult preVisitDirectory(IndexNode dir) throws IOException {
        if (dir.isRoot()) {
            preVisitDirectoryRoot(dir);
            return super.preVisitDirectory(dir);
        }

        IndexNode currentParent = currentStack.peek();
        IndexNode currentDir = currentParent.getChildByName(dir.getName());
        currentChildNamesVisited.get(currentParent).remove(dir.getName());

        if (currentDir == null) {
            removed(dir);
            return VisitorResult.SKIP;
        } else if (!dir.getNodeType().equals(currentDir.getNodeType())) {
            removed(dir);
            created(dir.getParent(), currentDir);
            return VisitorResult.SKIP;
        }

        IndexNodeChange nodeChange = changeFactory.createIndexNodeChange(dir, currentDir);
        if (nodeChange.getChange() == IndexNodeChange.Change.MODIFIED) {
            modified(nodeChange);
        }
        originStack.push(dir);
        currentStack.push(currentDir);

        Set<String> names = currentDir.getChildNames();
        currentChildNamesVisited.put(currentDir, names);
        return super.preVisitDirectory(dir);
    }

    private void preVisitDirectoryRoot(IndexNode root) {
        originStack.push(root);
        currentStack.push(currentRoot);

        IndexNodeChange rootChange = changeFactory.createIndexNodeChange(root, currentRoot);
        if (rootChange.getChange() == IndexNodeChange.Change.MODIFIED) {
            modified(rootChange);
        }

        Set<String> names = currentRoot.getChildNames();
        currentChildNamesVisited.put(currentRoot, names);
    }

    @Override
    public VisitorResult visitFile(IndexNode file) throws IOException {
        IndexNode otherParent = currentStack.peek();
        currentChildNamesVisited.get(otherParent).remove(file.getName());

        IndexNode otherFile = otherParent.getChildByName(file.getName());
        if (otherFile == null) {
            removed(file);
        } else if (!file.getNodeType().equals(otherFile.getNodeType())) {
            removed(file);
            created(file.getParent(), otherFile);
        } else {
            IndexNodeChange nodeChange = changeFactory.createIndexNodeChange(file, otherFile);
            if (nodeChange.getChange() == IndexNodeChange.Change.MODIFIED) {
                modified(nodeChange);
            }
        }

        return super.visitFile(file);
    }

    @Override
    public VisitorResult postVisitDirectory(IndexNode dir) throws IOException {
        IndexNode lastOriginDir = originStack.pop();
        IndexNode lastCurrentDir = currentStack.pop();
        Set<String> createdCurrentChildNames = currentChildNamesVisited.remove(lastCurrentDir);

        if (createdCurrentChildNames != null && !createdCurrentChildNames.isEmpty()) {
            for (String name : createdCurrentChildNames) {
                IndexNode newChild = lastCurrentDir.getChildByName(name);
                assert newChild != null;
                created(lastOriginDir, newChild);
            }
        }
        return super.postVisitDirectory(dir);
    }

    private void created(IndexNode originParent, IndexNode currentFile) {
        changes.add(new IndexNodeChange(IndexNodeChange.Change.CREATED, originParent, currentFile));
    }

    private void modified(IndexNodeChange change) {
        changes.add(change);
    }

    private void removed(IndexNode originFile) {
        changes.add(new IndexNodeChange(IndexNodeChange.Change.REMOVED, originFile, null));
    }
}
