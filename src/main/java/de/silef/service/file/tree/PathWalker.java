package de.silef.service.file.tree;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by sebastian on 17.09.16.
 */
public class PathWalker {

    public static void walk(Path base, Visitor<? super Path> visitor) throws IOException {
        if (!Files.isDirectory(base)) {
            return;
        }
        Visitor.VisitorResult result = visitor.preVisitDirectory(base);
        if (result != Visitor.VisitorResult.CONTINUE) {
            return;
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(base)) {
            List<Path> paths = StreamSupport.stream(directoryStream.spliterator(), false).sorted().collect(Collectors.toList());
            for (Path path : paths) {
                if (!Files.isReadable(path)) {
                    continue;
                }

                if (Files.isDirectory(path)) {
                    walk(path, visitor);
                } else {
                    visitor.visitFile(path);
                }
            }
        }
        visitor.postVisitDirectory(base);
    }

}
