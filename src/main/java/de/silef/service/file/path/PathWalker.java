package de.silef.service.file.path;

import de.silef.service.file.tree.Visitor;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static de.silef.service.file.tree.Visitor.VisitorResult.*;

/**
 * Created by sebastian on 17.09.16.
 */
public class PathWalker {

    public static Visitor.VisitorResult walk(PathInfo base, Visitor<? super PathInfo> visitor) throws IOException {
        if (!base.isDirectory()) {
            return SKIP;
        }

        Visitor.VisitorResult result = visitor.preVisitDirectory(base);
        if (result != CONTINUE) {
            return result;
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(base.getPath())) {
            List<PathInfo> pathInfos = StreamSupport.stream(directoryStream.spliterator(), false)
                    .map(PathInfo::create)
                    .sorted(sortByModeAndName())
                    .collect(Collectors.toList());

            for (PathInfo pathInfo : pathInfos) {
                if (pathInfo.isDirectory()) {
                    result = walk(pathInfo, visitor);
                } else {
                    result = visitor.visitFile(pathInfo);
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

    private static Comparator<PathInfo> sortByModeAndName() {
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
