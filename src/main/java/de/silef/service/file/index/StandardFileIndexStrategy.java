package de.silef.service.file.index;

import de.silef.service.file.change.IndexNodeChange;
import de.silef.service.file.change.IndexNodeChangeAnalyser;
import de.silef.service.file.extension.*;
import de.silef.service.file.node.IndexNode;
import de.silef.service.file.node.IndexNodeFactory;
import de.silef.service.file.path.IndexNodePathFactory;
import de.silef.service.file.node.IndexNodeType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.silef.service.file.extension.ExtensionType.*;

/**
 * Created by sebastian on 23.09.16.
 */
public class StandardFileIndexStrategy implements IndexNodeFactory, IndexNodePathFactory, IndexNodeChangeAnalyser, ImportPathFilter {

    @Override
    public IndexNode createFromIndex(IndexNode parent, IndexNodeType type, String name, List<IndexExtension> extensions) {
        List<IndexExtension> typedExtensions = new ArrayList<>(extensions.size());
        for (IndexExtension extension : extensions) {
            if (extension.getType() == UNIVERSAL_HASH.value) {
                typedExtensions.add(new UniversalHashIndexExtension(extension.getData()));
            } else if (extension.getType() == FILE_HASH.value) {
                typedExtensions.add(new FileContentHashIndexExtension(extension.getData()));
            } else if (extension.getType() == UNIX_FILE.value) {
                typedExtensions.add(new UnixFileIndexExtension(extension.getData()));
            } else {
                typedExtensions.add(extension);
            }
        }
        return new IndexNode(parent, type, name, typedExtensions);
    }

    @Override
    public IndexNode createFromPath(IndexNode parent, Path path) throws IOException {
        IndexNode node = IndexNode.createFromPath(parent, path);
        node.addExtension(BasicFileIndexExtension.createFromPath(path));
        node.addExtension(UnixFileIndexExtension.createFromPath(path));
        return node;
    }

    @Override
    public IndexNodeChange.Change analyse(IndexNode primary, IndexNode other) {
        if (primary.isRoot() && other.isRoot()) {
            return IndexNodeChange.Change.SAME;
        }

        IndexNodeChange.Change change = analyseExtension(ExtensionType.BASIC_FILE, primary, other);
        if (change != IndexNodeChange.Change.SAME) {
            return change;
        }
        return analyseExtension(ExtensionType.UNIX_FILE, primary, other);
    }

    private IndexNodeChange.Change analyseExtension(ExtensionType type, IndexNode primary, IndexNode other) {
        IndexExtension primaryExtension = primary.getExtensionByType(type.value);
        IndexExtension otherExtension = other.getExtensionByType(type.value);

        if (primaryExtension == null || otherExtension == null) {
            return IndexNodeChange.Change.MODIFIED;
        } else if (!Arrays.equals(primaryExtension.getData(), otherExtension.getData())) {
            return IndexNodeChange.Change.MODIFIED;
        }
        return IndexNodeChange.Change.SAME;
    }

    @Override
    public boolean importPath(Path path) {
        return true;
    }
}
