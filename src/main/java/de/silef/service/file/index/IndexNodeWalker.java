package de.silef.service.file.index;

import de.silef.service.file.tree.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static de.silef.service.file.tree.Visitor.VisitorResult.*;

/**
 * Created by sebastian on 20.09.16.
 */
public class IndexNodeWalker {

    private static final Logger LOG = LoggerFactory.getLogger(FileIndex.class);

    public static Visitor.VisitorResult walk(IndexNode node, Visitor<? super IndexNode> visitor) {
        try {
            return walkSuppressWarning(node, visitor);
        } catch (IOException e) {
            LOG.error("Failed to walk node {}", node.getRelativePath());
            return CONTINUE;
        }
    }

    private static Visitor.VisitorResult walkSuppressWarning(IndexNode node, Visitor<? super IndexNode> visitor) throws IOException {
        if (!node.getMode().isDirectory()) {
            return SKIP;
        }
        Visitor.VisitorResult result = visitor.preVisitDirectory(node);
        if (result != CONTINUE) {
            return result;
        }

        List<IndexNode> sortedChildren = new ArrayList<>(node.getChildren());
        sortedChildren.sort(sortByModeAndName());
        for (IndexNode child : sortedChildren) {
            if (child.getMode().isDirectory()) {
                result = walk(child, visitor);
            } else {
                result = visitor.visitFile(child);
            }
            if (result == SKIP_SIBLINGS || result == TERMINATE) {
                break;
            }
        }
        if (result == TERMINATE) {
            return TERMINATE;
        }
        return visitor.postVisitDirectory(node);
    }

    private static Comparator<IndexNode> sortByModeAndName() {
        return (a, b) -> {
            if (a.getMode().isDirectory() && !b.getMode().isDirectory()) {
                return -1;
            } else if (!a.getMode().isDirectory() && b.getMode().isDirectory()) {
                return 1;
            } else {
                return a.getName().compareTo(b.getName());
            }
        };
    }

}
