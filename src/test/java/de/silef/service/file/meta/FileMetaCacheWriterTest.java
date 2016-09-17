package de.silef.service.file.meta;

import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileMetaCacheWriterTest extends BasePathTest {

    @Test
    public void write() throws IOException {
        Path base = PathUtils.getResourcePath("meta");
        FileMetaCache cache = new FileMetaCache(base);


        new FileMetaCacheWriter().write(cache, tmp.resolve("filecache"));


        assertThat(Files.exists(tmp.resolve("filecache")), is(true));
    }

    @Test
    public void writeShouldBeRead() throws IOException {
        Path base = PathUtils.getResourcePath("meta/foo");
        FileMetaCache cache = new FileMetaCache(base);

        Path filecache = tmp.resolve("filecache");
        new FileMetaCacheWriter().write(cache, filecache);


        FileMetaCache readCache = new FileMetaCacheReader().read(base, filecache);


        assertThat(readCache.getChanges(cache).hasChanges(), is(false));
    }
}