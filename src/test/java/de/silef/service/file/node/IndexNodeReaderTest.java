package de.silef.service.file.node;

import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexNodeReaderTest extends BasePathTest {

    @Test
    public void read() throws IOException {
        Path base = Paths.get(".");
        Path file = PathUtils.getResourcePath("index/fileindex");


        IndexNode root = new IndexNodeReader(standardFileIndexStrategy).read(base, file);


        List<String> paths = root.stream().map(n -> n.getRelativePath().toString()).collect(Collectors.toList());
        assertThat(paths, is(Arrays.asList("", "bar", "bar/zoo.txt", "doe.txt")));
    }
}