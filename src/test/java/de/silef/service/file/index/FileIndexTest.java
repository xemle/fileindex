package de.silef.service.file.index;

import de.silef.service.file.hash.HashUtil;
import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by sebastian on 18.09.16.
 */
public class FileIndexTest extends BasePathTest {

    @Test
    public void init() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");


        FileIndex index = new FileIndex(base);


        verifyRootHash(index, "d20cacffb45a6814bc3d33492ee7924aaf54300d");
    }

    @Test
    public void initializeTreeHash() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");

        FileIndex index = new FileIndex(base);


        index.initializeTreeHash();


        verifyRootHash(index, "477f1ae1b076ace04a5d398687113a8c539f46a6");
    }

    private void verifyRootHash(FileIndex index, String hash) {
        assertThat(HashUtil.toHex(index.getRoot().getHash().getBytes()), is(hash));
    }
}