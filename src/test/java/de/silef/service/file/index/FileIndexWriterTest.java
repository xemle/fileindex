package de.silef.service.file.index;

import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileIndexWriterTest extends BasePathTest {

    @Test
    public void write() throws IOException {
        Path base = PathUtils.getResourcePath("meta");
        FileIndex cache = new FileIndex(base);


        new FileIndexWriter().write(cache, tmp.resolve("filecache"));


        assertThat(Files.exists(tmp.resolve("filecache")), is(true));
    }

    @Test
    public void writeShouldBeReadable() throws IOException {
        Path base = PathUtils.getResourcePath("meta/foo");
        FileIndex cache = new FileIndex(base);

        Path filecache = tmp.resolve("filecache");
        new FileIndexWriter().write(cache, filecache);


        FileIndex readCache = new FileIndexReader().read(base, filecache);


        assertThat(readCache.getChanges(cache).hasChanges(), is(false));
    }
}