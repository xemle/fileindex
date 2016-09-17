package de.silef.service.file.test;

import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by sebastian on 17.09.16.
 */
public abstract class BasePathTest {
    protected Path tmp;

    @Before
    public void setUp() throws IOException {
        tmp = Files.createTempDirectory("file-meta-cache-writer-");
    }

    @After
    public void tearDown() throws IOException {
        PathUtils.delete(tmp);
    }
}
