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


        verifyRootHash(index, "ba6816c8376dfa4d39fce6c0b9256908f1b083c5");
    }

    @Test
    public void initializeTreeHash() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");

        FileIndex index = new FileIndex(base);


        index.initializeTreeHash();


        verifyRootHash(index, "186d88652af97d938f8d550478ffe1d60e074775");
    }

    private void verifyRootHash(FileIndex index, String hash) {
        assertThat(HashUtil.toHex(index.getRoot().getHash().getBytes()), is(hash));
    }
}