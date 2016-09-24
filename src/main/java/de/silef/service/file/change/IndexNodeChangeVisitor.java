package de.silef.service.file.change;

import de.silef.service.file.node.IndexNode;
import de.silef.service.file.tree.Visitor;

import java.io.IOException;
import java.util.*;


/**
 * Created by sebastian on 24.09.16.
 */
public class IndexNodeChangeVisitor extends Visitor<IndexNode> {
    private List<IndexNodeChange> changes = new LinkedList<>();

    private IndexNode otherRoot;

    private Stack<IndexNode> primaryStack = new Stack<>();
    private Stack<IndexNode> otherStack = new Stack<>();

    private Map<IndexNode, Set<String>> otherChildNamesVisited = new HashMap<>();

    private IndexNodeChangeAnalyser changeInspector;

    public IndexNodeChangeVisitor(IndexNode otherRoot, IndexNodeChangeAnalyser changeInspector) {
        this.otherRoot = otherRoot;
        this.changeInspector = changeInspector;
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

        IndexNode otherParent = otherStack.peek();
        IndexNode otherDir = otherParent.getChildByName(dir.getName());
        otherChildNamesVisited.get(otherParent).remove(dir.getName());

        if (otherDir == null) {
            removed(dir);
            return VisitorResult.SKIP;
        } else if (!dir.getNodeType().equals(otherDir.getNodeType())) {
            removed(dir);
            created(dir.getParent(), otherDir);
            return VisitorResult.SKIP;
        }

        IndexNodeChange.Change change = changeInspector.analyse(dir, otherDir);
        if (change == IndexNodeChange.Change.MODIFIED) {
            modified(dir, otherDir);
        }
        primaryStack.push(dir);
        otherStack.push(otherDir);

        Set<String> names = otherDir.getChildNames();
        otherChildNamesVisited.put(otherDir, names);
        return super.preVisitDirectory(dir);
    }

    private void preVisitDirectoryRoot(IndexNode root) {
        primaryStack.push(root);
        otherStack.push(otherRoot);

        IndexNodeChange.Change change = changeInspector.analyse(root, otherRoot);
        if (change == IndexNodeChange.Change.MODIFIED) {
            modified(root, otherRoot);
        }

        Set<String> names = otherRoot.getChildNames();
        otherChildNamesVisited.put(otherRoot, names);
    }

    @Override
    public VisitorResult visitFile(IndexNode file) throws IOException {
        IndexNode otherParent = otherStack.peek();
        otherChildNamesVisited.get(otherParent).remove(file.getName());

        IndexNode otherFile = otherParent.getChildByName(file.getName());
        if (otherFile == null) {
            removed(file);
        } else if (!file.getNodeType().equals(otherFile.getNodeType())) {
            removed(file);
            created(file.getParent(), otherFile);
        } else {
            IndexNodeChange.Change change = changeInspector.analyse(file, otherFile);
            if (change == IndexNodeChange.Change.MODIFIED) {
                modified(file, otherFile);
            }
        }

        return super.visitFile(file);
    }

    @Override
    public VisitorResult postVisitDirectory(IndexNode dir) throws IOException {
        IndexNode lastPrimaryDir = primaryStack.pop();
        IndexNode lastOtherDir = otherStack.pop();
        Set<String> createdOtherChildNames = otherChildNamesVisited.remove(lastOtherDir);

        if (createdOtherChildNames != null && !createdOtherChildNames.isEmpty()) {
            for (String name : createdOtherChildNames) {
                IndexNode newChild = lastOtherDir.getChildByName(name);
                assert newChild != null;
                created(lastPrimaryDir, newChild);
            }
        }
        return super.postVisitDirectory(dir);
    }

    private void created(IndexNode parent, IndexNode otherFile) {
        changes.add(new IndexNodeChange(IndexNodeChange.Change.CREATED, parent, otherFile));
    }

    private void modified(IndexNode file, IndexNode otherFile) {
        changes.add(new IndexNodeChange(IndexNodeChange.Change.MODIFIED, file, otherFile));
    }

    private void removed(IndexNode file) {
        changes.add(new IndexNodeChange(IndexNodeChange.Change.REMOVED, file, null));
    }
}
