package de.silef.service.file.path;

import de.silef.service.file.tree.Visitor;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static de.silef.service.file.tree.Visitor.VisitorResult.*;

/**
 * Created by sebastian on 17.09.16.
 */
public class PathWalker {

    public static Visitor.VisitorResult walk(PathAttribute base, Visitor<? super PathAttribute> visitor) throws IOException {
        if (!base.isDirectory()) {
            return SKIP;
        }

        Visitor.VisitorResult result = visitor.preVisitDirectory(base);
        if (result != CONTINUE) {
            return result;
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(base.getPath())) {
            List<PathAttribute> paths = StreamSupport.stream(directoryStream.spliterator(), false)
                    .map(PathAttribute::create)
                    .sorted(sortByModeAndName())
                    .collect(Collectors.toList());

            for (PathAttribute path : paths) {
                if (path.isDirectory()) {
                    result = walk(path, visitor);
                } else {
                    result = visitor.visitFile(path);
                }
                if (result == SKIP_SIBLINGS || result == TERMINATE) {
                    break;
                }
            }
        }
        if (result == TERMINATE) {
            return TERMINATE;
        }
        return visitor.postVisitDirectory(base);
    }

    private static Comparator<PathAttribute> sortByModeAndName() {
        return (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) {
                return -1;
            } else if (!a.isDirectory() && b.isDirectory()) {
                return 1;
            } else {
                return a.getFileName().compareTo(b.getFileName());
            }
        };
    }

}
