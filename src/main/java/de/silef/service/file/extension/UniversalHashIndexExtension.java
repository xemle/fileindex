package de.silef.service.file.extension;

import de.silef.service.file.node.IndexNode;
import de.silef.service.file.util.HashUtil;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import static de.silef.service.file.extension.ExtensionType.BASIC_FILE;
import static de.silef.service.file.extension.ExtensionType.FILE_HASH;
import static de.silef.service.file.extension.ExtensionType.UNIVERSAL_HASH;

/**
 * System independent hash tree extension. The hash depends on node type, file size and file content. The
 * node type is system independent, too by FILE, DIRECTORY and LINK.
 *
 * The extension is only for directory nodes and depends on the FileContentHashIndexExtension for files
 * and (symbolic) links. If no FileContentHashIndexExtension could not be found for a node it uses an
 * empty hash for this node.
 */
public class UniversalHashIndexExtension extends StandardIndexExtension {

    private static final int DATA_SIZE = 20;

    private static final byte[] EMPTY_HASH = "00000000000000000000".getBytes();

    public UniversalHashIndexExtension(byte[] data) {
        super(UNIVERSAL_HASH.value, data);
        if (data.length < DATA_SIZE) {
            throw new IndexExtensionInvalidDataException("Invalid data length for Content Extension");
        }
    }

    public static UniversalHashIndexExtension create(IndexNode node) throws IOException {
        if (!node.isDirectory() && !node.isLink()) {
            throw new IllegalArgumentException("Universal hash is only allowed on directory nodes");
        }

        List<String> names = node.getChildNames().stream().sorted().collect(Collectors.toList());
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             DataOutputStream dataOutput = new DataOutputStream(output)) {
            for (String name : names) {
                IndexNode child = node.getChildByName(name);
                assert child != null;

                byte[] childHash = getChildHash(child);
                dataOutput.write(childHash);
                dataOutput.writeByte(child.getNodeType().getByte());
                dataOutput.writeUTF(child.getName());
            }
            byte[] hash = HashUtil.getHash(output.toByteArray());
            return new UniversalHashIndexExtension(hash);
        }
    }

    private static byte[] getChildHash(IndexNode child) throws IOException {
        if (child.isLink() || child.isFile()) {
            if (child.hasExtensionType(FILE_HASH.value)) {
                return child.getExtensionByType(FILE_HASH.value).getData();
            }
            return EMPTY_HASH;
        } else if (child.isDirectory()) {
            if (!child.hasExtensionType(UNIVERSAL_HASH.value)) {
                UniversalHashIndexExtension extension = create(child);
                child.addExtension(extension);
            }
            return child.getExtensionByType(UNIVERSAL_HASH.value).getData();
        }
        return EMPTY_HASH;
    }

    @Override
    public String toString() {
        return "UniversalHash{" + HashUtil.toHex(getData()) + "}";
    }
}
