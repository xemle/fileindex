package de.silef.service.file.node;

import de.silef.service.file.index.FileIndex;
import de.silef.service.file.change.IndexChange;
import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexNodeWriterTest extends BasePathTest {

    @Test
    public void write() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        FileIndex index = FileIndex.create(base, indexStrategy);


        new IndexNodeWriter().write(index.getRoot(), tmp.resolve("fileindex"));


        assertThat(Files.exists(tmp.resolve("fileindex")), is(true));
    }

    @Test
    public void writeShouldBeReadable() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        FileIndex cache = FileIndex.create(base, indexStrategy);

        Path fileindex = tmp.resolve("fileindex");
        new IndexNodeWriter().write(cache.getRoot(), fileindex);


        IndexNode root = new IndexNodeReader(indexStrategy).read(fileindex);


        FileIndex index = new FileIndex(base, root);
        IndexChange change = index.getChanges(cache, indexStrategy);
        assertThat(change.hasChanges(), is(false));
    }
}