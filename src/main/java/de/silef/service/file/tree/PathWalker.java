package de.silef.service.file.tree;

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

    public static Visitor.VisitorResult walk(Path base, Visitor<? super Path> visitor) throws IOException {
        if (!Files.isDirectory(base)) {
            return SKIP;
        }
        Visitor.VisitorResult result = visitor.preVisitDirectory(base);
        if (result != CONTINUE) {
            return result;
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(base)) {
            List<Path> paths = StreamSupport.stream(directoryStream.spliterator(), false)
                    .sorted(sortByModeAndName())
                    .collect(Collectors.toList());

            for (Path path : paths) {
                if (!Files.isReadable(path)) {
                    continue;
                }

                if (Files.isDirectory(path)) {
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

    private static Comparator<Path> sortByModeAndName() {
        Map<Path, Boolean> isDirCache = new HashMap<>();
        return (a, b) -> {
            if (!isDirCache.containsKey(a)) {
                isDirCache.put(a, Files.isDirectory(a));
            }
            if (!isDirCache.containsKey(b)) {
                isDirCache.put(b, Files.isDirectory(b));
            }
            boolean isADir = isDirCache.get(a);
            boolean isBDir = isDirCache.get(b);
            if (isADir && !isBDir) {
                return -1;
            } else if (!isADir && isBDir) {
                return 1;
            } else {
                return a.compareTo(b);
            }
        };
    }

}
