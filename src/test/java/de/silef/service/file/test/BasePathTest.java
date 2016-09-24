package de.silef.service.file.test;

import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by sebastian on 17.09.16.
 */
public abstract class BasePathTest extends BaseTest {
    protected Path tmp;

    @Before
    public void setUp() throws IOException {
        super.setUp();
        tmp = Files.createTempDirectory("file-index-test-");
    }

    @After
    public void tearDown() throws IOException {
        PathUtils.delete(tmp);
    }
}
