package de.silef.service.file.index;

import de.silef.service.file.change.IndexNodeChange;
import de.silef.service.file.change.IndexNodeChangeAnalyser;
import de.silef.service.file.extension.*;
import de.silef.service.file.node.IndexNode;
import de.silef.service.file.node.IndexNodeFactory;
import de.silef.service.file.node.IndexNodeType;
import de.silef.service.file.path.CreatePathFilter;
import de.silef.service.file.path.IndexNodePathFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.silef.service.file.extension.ExtensionType.*;

/**
 * Created by sebastian on 23.09.16.
 */
public class StandardFileIndexStrategy implements IndexNodeFactory, IndexNodePathFactory, IndexNodeChangeAnalyser, CreatePathFilter {

    @Override
    public IndexNode createFromIndex(IndexNode parent, IndexNodeType type, String name, List<IndexExtension> extensions) {
        List<IndexExtension> typedExtensions = new ArrayList<>(extensions.size());
        for (IndexExtension extension : extensions) {
            if (extension.getType() == BASIC_FILE.value) {
                typedExtensions.add(new BasicFileIndexExtension(extension.getData()));
            } else if (extension.getType() == UNIVERSAL_HASH.value) {
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
    public boolean isValidPath(Path path, BasicFileAttributes attributes) {
        return true;
    }

    @Override
    public IndexNode createFromPath(IndexNode parent, Path path, BasicFileAttributes attributes) throws IOException {
        IndexNodeType type = IndexNodeType.create(attributes.isDirectory(), attributes.isRegularFile(), attributes.isSymbolicLink());
        IndexNode node = new IndexNode(parent, type, path.getFileName().toString());
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
