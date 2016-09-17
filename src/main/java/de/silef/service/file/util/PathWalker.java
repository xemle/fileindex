package de.silef.service.file.util;

import de.silef.service.file.util.PathVisitor.VisitorResult;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static de.silef.service.file.util.PathVisitor.VisitorResult.CONTINUE;

/**
 * Created by sebastian on 17.09.16.
 */
public class PathWalker {

    public static void walk(Path base, PathVisitor pathVisitor) throws IOException {
        if (!Files.isDirectory(base)) {
            return;
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(base)) {
            List<Path> paths = StreamSupport.stream(directoryStream.spliterator(), false).collect(Collectors.toList());
            for (Path path : paths) {
                if (!Files.isReadable(path)) {
                    continue;
                }

                if (Files.isDirectory(path)) {
                    VisitorResult result = pathVisitor.preVisitDirectory(path);
                    if (result == CONTINUE) {
                        walk(path, pathVisitor);
                    }
                } else {
                    pathVisitor.visitFile(path);
                }
            }
        }
    }

}
