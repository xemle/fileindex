package de.silef.service.file.change;

import de.silef.service.file.node.IndexNode;
import de.silef.service.file.node.IndexNodeWalker;

import java.nio.file.Path;

/**
 * Created by sebastian on 25.09.16.
 */
public class IndexChangeCreator {

    private IndexNodeChangeFactory changeFactory;

    public IndexChangeCreator(IndexNodeChangeFactory changeFactory) {
        this.changeFactory = changeFactory;
    }

    public IndexChange create(Path base, IndexNode origin, IndexNode update) {
        IndexNodeChangeVisitor visitor = new IndexNodeChangeVisitor(update, changeFactory);
        IndexNodeWalker.walk(origin, visitor);
        return new IndexChange(base, visitor.getChanges());
    }
}
