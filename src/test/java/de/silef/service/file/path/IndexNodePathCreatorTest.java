package de.silef.service.file.path;

import de.silef.service.file.node.IndexNode;
import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by sebastian on 25.09.16.
 */
public class IndexNodePathCreatorTest extends BasePathTest {

    private IndexNodePathCreator pathCreator;

    @Before
    public void setUp() throws IOException {
        super.setUp();

        pathCreator = new IndexNodePathCreator(standardFileIndexStrategy);
    }

    @Test
    public void create() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");


        IndexNode root = pathCreator.create(base, p -> true);


        verifyNodeCount(root, 4);
    }

    @Test
    public void createWithAbsoluteSymbolicLinkDir() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        PathUtils.copy(base, tmp);
        Files.createSymbolicLink(tmp.resolve("bar/link"), tmp);


        IndexNode root = pathCreator.create(tmp, p -> true);


        verifyNodeCount(root, 5);
    }

    @Test
    public void createWithRelativeSymbolicLinkDir() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        PathUtils.copy(base, tmp);
        Files.createSymbolicLink(tmp.resolve("bar/link"), Paths.get(".."));


        IndexNode root = pathCreator.create(tmp, p -> true);


        verifyNodeCount(root, 5);
    }

    @Test
    public void createWithAbsoluteSymbolicLinkFile() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        PathUtils.copy(base, tmp);
        Files.createSymbolicLink(tmp.resolve("bar/link.txt"), tmp.resolve("doe.txt"));


        IndexNode root = pathCreator.create(tmp, p -> true);


        verifyNodeCount(root, 5);
    }

    @Test
    public void createWithRelativeSymbolicLinkFile() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        PathUtils.copy(base, tmp);
        Files.createSymbolicLink(tmp.resolve("bar/link.txt"), Paths.get("zoo.txt"));


        IndexNode root = pathCreator.create(tmp, p -> true);


        verifyNodeCount(root, 5);
    }

    @Test
    public void createWithSymbolicLinkFileToNonExistingTarget() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        PathUtils.copy(base, tmp);
        Files.createSymbolicLink(tmp.resolve("bar/link.txt"), Paths.get("ghost.txt"));


        IndexNode root = pathCreator.create(tmp, p -> true);


        verifyNodeCount(root, 5);
    }

    private void verifyNodeCount(IndexNode root, long expectedCount) {
        assertThat(root.stream().count(), is(expectedCount));
    }
}