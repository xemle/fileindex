package de.silef.service.file.test;

import de.silef.service.file.index.StandardFileIndexStrategy;
import org.junit.Before;

import java.io.IOException;

/**
 * Created by sebastian on 23.09.16.
 */
public class BaseTest {

    protected StandardFileIndexStrategy standardFileIndexStrategy;

    @Before
    public void setUp() throws IOException {
        standardFileIndexStrategy = new StandardFileIndexStrategy();
    }
}
