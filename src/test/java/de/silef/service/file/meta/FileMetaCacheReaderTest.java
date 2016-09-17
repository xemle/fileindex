package de.silef.service.file.meta;

import de.silef.service.file.test.PathUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileMetaCacheReaderTest {

    @Test
    public void read() throws IOException {
        Path base = Paths.get(".");
        Path file = PathUtils.getResourcePath("meta/filecache");


        FileMetaCache cache = new FileMetaCacheReader().read(base, file);


        assertThat(cache.getPaths().size(), is(3));
    }
}