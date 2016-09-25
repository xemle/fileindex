package de.silef.service.file.node;

import de.silef.service.file.extension.IndexExtension;
import de.silef.service.file.tree.Visitor;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexNode implements Serializable {

    private IndexNode parent = null;

    private IndexNodeType nodeType;

    private Map<Byte, IndexExtension> typeToExtension = new HashMap<>();

    private List<IndexNode> children = new ArrayList<>();
    private Map<String, IndexNode> nameToChild = new HashMap<>();

    private String name;

    private Path relativePath = null;

    public IndexNode(IndexNode parent, IndexNodeType nodeType, String name) {
        this(parent, nodeType, name, null);
    }

    public IndexNode(IndexNode parent, IndexNodeType nodeType, String name, List<IndexExtension> extensions) {
        this.parent = parent;
        this.nodeType = nodeType;
        this.name = name;
        this.typeToExtension = makeExtensionMap(extensions);
    }

    private Map<Byte, IndexExtension> makeExtensionMap(List<IndexExtension> extensions) {
        Map<Byte, IndexExtension> typeToExtension = new HashMap<>();
        if (extensions == null) {
            return typeToExtension;
        }
        for (IndexExtension extension : extensions) {
            typeToExtension.put(extension.getType(), extension);
        }
        return typeToExtension;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public IndexNode getParent() {
        return parent;
    }

    void setParent(IndexNode parent) {
        this.parent = parent;
        this.relativePath = null;
    }

    public IndexNodeType getNodeType() {
        return nodeType;
    }

    public boolean isDirectory() {
        return getNodeType().isDirectory();
    }

    public boolean isFile() {
        return getNodeType().isFile();
    }

    public boolean isLink() {
        return getNodeType().isLink();
    }

    public String getName() {
        return name;
    }

    public List<IndexExtension> getExtensions() {
        return new ArrayList<>(typeToExtension.values());
    }

    public IndexExtension getExtensionByType(byte type) {
        return typeToExtension.get(type);
    }

    public boolean hasExtensionType(byte type) {
        return typeToExtension.containsKey(type);
    }

    public void addExtension(IndexExtension extension) {
        typeToExtension.put(extension.getType(), extension);
    }

    public void addAllExtensions(List<IndexExtension> extensions) {
        for (IndexExtension extension : extensions) {
            addExtension(extension);
        }
    }

    public IndexExtension removeExtensionType(byte type) {
        return typeToExtension.remove(type);
    }

    public List<IndexNode> getChildren() {
        return new ArrayList<>(children);
    }

    public void addChild(IndexNode node) {
        IndexNode nodeParent = node.getParent();
        if (nodeParent != null) {
            node.getParent().removeChildByName(node.getName());
        }
        node.setParent(this);

        IndexNode oldNode = removeChildByName(node.getName());
        if (oldNode != null) {
            oldNode.setParent(null);
        }
        nameToChild.put(node.getName(), node);
        children.add(node);
    }

    public void setChildren(List<IndexNode> children) {
        this.children = new ArrayList<>(children);
        nameToChild = new HashMap<>();
        for (IndexNode child : children) {
            child.setParent(this);
            nameToChild.put(child.getName(), child);
        }
    }

    public IndexNode removeChildByName(String name) {
        IndexNode node = getChildByName(name);
        children.remove(node);
        nameToChild.remove(name);
        return node;
    }

    public IndexNode getChildByName(String name) {
        return nameToChild.get(name);
    }

    public Set<String> getChildNames() {
        return new HashSet<>(nameToChild.keySet());
    }

    public Path getRelativePath() {
        if (relativePath == null) {
            relativePath = parent == null ? Paths.get("") : parent.getRelativePath().resolve(name);
        }
        return relativePath;
    }

    public Stream<IndexNode> stream() {
        Stream.Builder<IndexNode> streamConsumer = Stream.builder();
        IndexNodeWalker.walk(this, new Visitor<IndexNode>() {
            @Override
            public VisitorResult preVisitDirectory(IndexNode dir) throws IOException {
                streamConsumer.accept(dir);
                return super.preVisitDirectory(dir);
            }

            @Override
            public VisitorResult visitFile(IndexNode file) throws IOException {
                streamConsumer.accept(file);
                return super.visitFile(file);
            }
        });
        return streamConsumer.build();
    }

    @Override
    public String toString() {
        return nodeType + " " + getRelativePath();
    }

}
