package de.silef.service.file.index;

import de.silef.service.file.change.IndexNodeChange;
import de.silef.service.file.change.IndexNodeChangeAnalyser;
import de.silef.service.file.extension.*;
import de.silef.service.file.node.IndexNode;
import de.silef.service.file.node.IndexNodeFactory;
import de.silef.service.file.node.IndexNodeType;
import de.silef.service.file.path.PathInfoFilter;
import de.silef.service.file.path.IndexNodePathFactory;
import de.silef.service.file.path.PathInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sebastian on 23.09.16.
 */
public class StandardFileIndexStrategy implements IndexNodeFactory, IndexNodePathFactory, IndexNodeChangeAnalyser, PathInfoFilter {

    @Override
    public IndexNode createIndexNode(IndexNode parent, IndexNodeType type, String name, List<IndexExtension> extensions) {
        return new IndexNode(parent, type, name, extensions);
    }

    @Override
    public IndexExtension createExtension(byte type, byte[] data) {
        ExtensionType extensionType = ExtensionType.fromByte(type);
        switch (extensionType) {
            case BASIC_FILE:
                return new BasicFileIndexExtension(data);
            case UNIX_FILE:
                return new UnixFileIndexExtension(data);
            case FILE_HASH:
                return new FileContentHashIndexExtension(data);
            case UNIVERSAL_HASH:
                return new UniversalHashIndexExtension(data);
            default:
                return new StandardIndexExtension(type, data);
        }
    }

    @Override
    public boolean isValidPathInfo(PathInfo pathInfo) {
        return true;
    }

    @Override
    public PathInfo createPathInfo(Path path)  {
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            return new PathInfo(path, attributes);
        } catch (IOException e) {
            throw new RuntimeException("Could not read attributes from path " + path, e);
        }
    }

    @Override
    public IndexNode createIndexNode(IndexNode parent, PathInfo pathInfo) throws IOException {
        BasicFileAttributes attributes = pathInfo.getAttributes();
        IndexNodeType type = IndexNodeType.create(attributes.isDirectory(), attributes.isRegularFile(), attributes.isSymbolicLink());
        IndexNode node = new IndexNode(parent, type, pathInfo.getFileName());
        if (!attributes.isOther()) {
            node.addExtension(BasicFileIndexExtension.createFromAttributes(attributes));
            // Do not use UnixFile extension in favour of speed. You can save about 60%
            //node.addExtension(UnixFileIndexExtension.createFromPath(path));
        }
        return node;
    }

    @Override
    public IndexNodeChange.Change analyse(IndexNode primary, IndexNode other) {
        if (primary.isRoot() && other.isRoot()) {
            return IndexNodeChange.Change.SAME;
        }

        return analyseExtension(ExtensionType.BASIC_FILE, primary, other);
    }

    private IndexNodeChange.Change analyseExtension(ExtensionType type, IndexNode primary, IndexNode other) {
        IndexExtension primaryExtension = primary.getExtensionByType(type.value);
        IndexExtension otherExtension = other.getExtensionByType(type.value);

        if (primaryExtension != null && otherExtension != null && !Arrays.equals(primaryExtension.getData(), otherExtension.getData())) {
            return IndexNodeChange.Change.MODIFIED;
        }
        return IndexNodeChange.Change.SAME;
    }
}
