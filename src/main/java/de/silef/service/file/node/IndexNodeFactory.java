package de.silef.service.file.node;

import de.silef.service.file.extension.IndexExtension;

import java.util.List;

/**
 * Created by sebastian on 23.09.16.
 */
public interface IndexNodeFactory {

    IndexNode createIndexNode(IndexNode parent, IndexNodeType type, String name, List<IndexExtension> extensions);
    
    IndexExtension createExtension(byte type, byte[] data);

}
