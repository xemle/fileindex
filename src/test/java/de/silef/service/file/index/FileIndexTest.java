package de.silef.service.file.index;

import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by sebastian on 18.09.16.
 */
public class FileIndexTest extends BasePathTest {

    @Test
    public void create() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");


        FileIndex index = FileIndex.create(base, standardFileIndexStrategy);


        assertThat(index.getTotalFileCount(), is(4L));
        assertThat(index.getTotalFileSize(), is(8L));
    }

}