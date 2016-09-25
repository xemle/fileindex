package de.silef.service.file.node;

import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by sebastian on 24.09.16.
 */
public class IndexNodeTypeTest extends BasePathTest {

    @Test
    public void basic() {
        assertTrue(IndexNodeType.DIRECTORY.isDirectory());
        assertFalse(IndexNodeType.DIRECTORY.isFile());
        assertFalse(IndexNodeType.DIRECTORY.isLink());
        assertFalse(IndexNodeType.DIRECTORY.isOther());

        assertFalse(IndexNodeType.FILE.isDirectory());
        assertTrue(IndexNodeType.FILE.isFile());
        assertFalse(IndexNodeType.FILE.isLink());
        assertFalse(IndexNodeType.FILE.isOther());

        assertFalse(IndexNodeType.SYMLINK.isDirectory());
        assertFalse(IndexNodeType.SYMLINK.isFile());
        assertTrue(IndexNodeType.SYMLINK.isLink());
        assertFalse(IndexNodeType.SYMLINK.isOther());
    }

    @Test
    public void symbolicLinkFile() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        PathUtils.copy(base, tmp);

        Path link = tmp.resolve("link.txt");
        Files.createSymbolicLink(link, tmp.resolve("doe.txt"));


        IndexNodeType type = IndexNodeType.create(link);


        assertThat(type.isDirectory(), is(false));
        assertThat(type.isFile(), is(true));
        assertThat(type.isLink(), is(true));
    }

    @Test
    public void symbolicLinkDir() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        PathUtils.copy(base, tmp);

        Path link = tmp.resolve("link");
        Files.createSymbolicLink(link, tmp.resolve("bar"));


        IndexNodeType type = IndexNodeType.create(link);


        assertThat(type.isDirectory(), is(true));
        assertThat(type.isFile(), is(false));
        assertThat(type.isLink(), is(true));
    }

    @Test
    public void symbolicLinkUnknownTarget() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        PathUtils.copy(base, tmp);

        Path link = tmp.resolve("link.txt");
        Files.createSymbolicLink(link, tmp.resolve("ghost.target"));


        IndexNodeType type = IndexNodeType.create(link);


        assertThat(type.isDirectory(), is(false));
        assertThat(type.isFile(), is(false));
        assertThat(type.isLink(), is(true));
    }
}