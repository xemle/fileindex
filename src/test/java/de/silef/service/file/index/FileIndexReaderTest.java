package de.silef.service.file.index;

import de.silef.service.file.test.PathUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndexReaderTest {

    @Test
    public void read() throws IOException {
        Path base = Paths.get(".");
        Path file = PathUtils.getResourcePath("index/fileindex");


        FileIndex cache = new FileIndexReader().read(base, file);


        List<String> paths = cache.getRoot().stream().map(n -> n.getRelativePath().toString()).collect(Collectors.toList());
        assertThat(paths, is(Arrays.asList("", "bar", "bar/zoo.txt", "doe.txt")));
    }
}