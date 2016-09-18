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
        Path base = PathUtils.getResourcePath("index");


        FileIndex index = new FileIndex(base);


        verifyRootHash(index, "91de5f07876f11efa1d354d99ebeaf7c8c52d516");
    }

    @Test
    public void initializeTreeHash() throws IOException {
        Path base = PathUtils.getResourcePath("index");

        FileIndex index = new FileIndex(base);


        index.initializeTreeHash();


        verifyRootHash(index, "3871e13db30feaef054f8073546ed8a8e63a9603");
    }

    private void verifyRootHash(FileIndex index, String hash) {
        assertThat(HashUtil.toHex(index.getRoot().getHash().getBytes()), is(hash));
    }
}