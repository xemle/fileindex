package de.silef.service.file.index;

import de.silef.service.file.meta.FileMetaChanges;
import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import de.silef.service.file.util.HashUtil;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndexReaderTest extends BasePathTest {

    @Test
    public void read() throws IOException {
        Path indexFile = tmp.resolve("index");
        Path base = PathUtils.getResourcePath("meta");
        FileIndex index = new FileIndex(base);

        FileMetaChanges changes = new FileMetaChanges(base, new HashSet<>(Arrays.asList("foo/doe.txt")), new HashSet<>(), new HashSet<>());
        index.updateChanges(changes, false);

        new FileIndexWriter().write(index, indexFile);


        index = new FileIndexReader().read(base, indexFile);


        assertThat(HashUtil.toHex(index.getRoot().getHash()), is("c764b5aabf3643a286e974725c124a4a63df4aab"));
    }

    @Test
    public void readTest() throws IOException {
        Path indexFile = PathUtils.getResourcePath("fileindex");

        FileIndex index = new FileIndexReader().read(tmp, indexFile);
    }
}