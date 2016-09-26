package de.silef.service.file.node;

import de.silef.service.file.extension.IndexExtension;
import de.silef.service.file.extension.StandardIndexExtension;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

import static de.silef.service.file.node.IndexNodeWriter.MAGIC_HEADER;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexNodeReader {

    private IndexNodeFactory nodeFactory;

    public IndexNodeReader(IndexNodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public IndexNode read(Path base, Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            return read(input);
        }
    }

    private IndexNode read(InputStream input) throws IOException {
        try (InflaterInputStream inflaterInput = new InflaterInputStream(input);
             BufferedInputStream bufferedInput = new BufferedInputStream(inflaterInput);
             DataInputStream dataInput = new DataInputStream(bufferedInput)) {

            int header = dataInput.readInt();
            if (header != MAGIC_HEADER) {
                throw new IOException("Unexpected header: " + header);
            }
            return readNode(null, dataInput);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("Could not read cache nodes", e);
        }
    }

    private IndexNode readNode(IndexNode parent, DataInputStream dataInput)
            throws ClassNotFoundException, IOException {

        IndexNodeType type = IndexNodeType.create(dataInput.readByte());
        String name = dataInput.readUTF();
        List<IndexExtension> extensions = readExtensions(dataInput);

        IndexNode node = nodeFactory.createFromIndex(parent, type, name, extensions);

        List<IndexNode> children = readChildren(dataInput, node);
        node.setChildren(children);

        return node;
    }

    private List<IndexNode> readChildren(DataInputStream input, IndexNode parent) throws IOException, ClassNotFoundException {
        int childrenCount = input.readInt();
        List<IndexNode> children = new ArrayList<>(childrenCount);
        for (int i = 0; i < childrenCount; i++) {
            children.add(readNode(parent, input));
        }
        return children;
    }

    private List<IndexExtension> readExtensions(DataInputStream input) throws IOException {
        int extensionCount = input.readByte() & 0xff;
        List<IndexExtension> extensions = new ArrayList<>(extensionCount);
        for (int i = 0; i < extensionCount; i++) {
            byte type = input.readByte();
            int size = input.readShort();
            byte[] data = new byte[size];
            if (input.read(data) != size) {
                throw new IOException("Could not read extension data");
            }
            extensions.add(nodeFactory.createExtensionFromIndex(type, data));
        }
        return extensions;
    }
}
