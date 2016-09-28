package de.silef.service.file.change;

import de.silef.service.file.index.FileIndex;
import de.silef.service.file.node.IndexNode;
import de.silef.service.file.node.IndexNodeWalker;

import java.nio.file.Path;

/**
 * Created by sebastian on 25.09.16.
 */
public class IndexChangeCreator {

    private IndexNodeChangeAnalyser changeAnalyser;

    public IndexChangeCreator(IndexNodeChangeAnalyser changeAnalyser) {
        this.changeAnalyser = changeAnalyser;
    }

    public IndexChange create(Path base, IndexNode origin, IndexNode update) {
        IndexNodeChangeVisitor visitor = new IndexNodeChangeVisitor(update, changeAnalyser);
        IndexNodeWalker.walk(origin, visitor);
        return new IndexChange(base, visitor.getChanges());
    }
}
