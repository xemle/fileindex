package de.silef.service.file.change;

import de.silef.service.file.node.IndexNode;

/**
 * Created by sebastian on 24.09.16.
 */
public interface IndexNodeChangeFactory {

    IndexNodeChange createIndexNodeChange(IndexNode origin, IndexNode current);
}
