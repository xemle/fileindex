package de.silef.service.file.index;

import de.silef.service.file.meta.FileMetaChanges;
import de.silef.service.file.meta.FileMode;
import de.silef.service.file.util.HashUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndex {

    public static int MAGIC_HEADER = 0x08020305;

    private IndexNode root;

    private Path base;

    public FileIndex(Path base) {
        this(base, new IndexNode("", new LinkedList<>()));
    }

    public FileIndex(Path base, IndexNode root) {
        this.base = base;
        this.root = root;
    }

    public void init(Collection<String> paths) throws IOException {
        updateAll(paths, false);
    }

    public void init(Collection<String> paths, boolean suppressErrors) throws IOException {
        updateAll(paths, suppressErrors);
    }

    public void updateChanges(FileMetaChanges changes, boolean suppressErrors) throws IOException {
        if (!changes.hasChanges()) {
            return;
        }
        updateAll(changes.getCreated(), suppressErrors);
        updateAll(changes.getModified(), suppressErrors);
        removeAll(changes.getRemoved());
    }

    private void updateAll(Collection<String> paths) throws IOException {
        updateAll(paths, false);
    }

    private void updateAll(Collection<String> paths, boolean suppressErrors) throws IOException {
        for (String path : paths) {
            Path file = base.resolve(path);
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            try {
                byte[] hash = HashUtil.getHash(file);
                insertNode(path, hash);
            } catch (IOException e) {
                if (!suppressErrors) {
                    throw e;
                }
            }
        }
    }


    private void insertNode(String nodePath, byte[] hash) throws IOException {
        Path path = Paths.get(nodePath);
        List<String> names = new LinkedList<>();
        for (int i = 0; i < path.getNameCount(); i++) {
            names.add(path.getName(i).toString());
        }
        root = insertNode(root, "", names, hash);
    }

    private IndexNode insertNode(IndexNode node, String nodeName, List<String> names, byte[] hash) throws IOException {
        if (node.getFileMode() != FileMode.DIRECTORY) {
            throw new IllegalArgumentException("node must be an directory");
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Path names must not be empty");
        }

        String name = names.remove(0);
        IndexNode child = node.findChildren(name);
        List<IndexNode> children = node.getChildren();
        children.remove(child);


        if (names.isEmpty()) {
            children.add(new IndexNode(name, hash));
        } else {
            if (child == null) {
                child = new IndexNode(name, new LinkedList<>());
            }
            children.add(insertNode(child, name, names, hash));
        }

        children.sort((a, b) -> a.getName().compareTo(b.getName()));
        return new IndexNode(nodeName, children);
    }

    private void removeNode(String nodePath) throws IOException {
        Path path = Paths.get(nodePath);
        List<String> names = new LinkedList<>();
        for (int i = 0; i < path.getNameCount(); i++) {
            names.add(path.getName(i).toString());
        }
        root = removeNode(root, "", names);
    }

    private IndexNode removeNode(IndexNode node, String nodeName, List<String> names) throws IOException {
        if (node.getFileMode() != FileMode.DIRECTORY) {
            throw new IllegalArgumentException("node must be an directory");
        }
        String name = names.remove(0);
        IndexNode child = node.findChildren(name);
        List<IndexNode> children = node.getChildren();
        children.remove(child);

        if (!names.isEmpty() && child != null) {
            children.add(removeNode(child, name, names));
        }

        children.sort((a, b) -> a.getName().compareTo(b.getName()));
        return new IndexNode(nodeName, children);
    }

    private void removeAll(Collection<String> paths) throws IOException {
        for (String path : paths) {
            removeNode(path);
        }
    }

    IndexNode getRoot() {
        return root;
    }
}
