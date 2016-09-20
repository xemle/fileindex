package de.silef.service.file.node;

import de.silef.service.file.hash.FileHash;
import de.silef.service.file.util.HashUtil;
import de.silef.service.file.test.PathUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by sebastian on 18.09.16.
 */
public class IndexNodeTest {

    @Test
    public void createFromPathOfFile() throws IOException {
        Path file = PathUtils.getResourcePath("index/foo/doe.txt");
        IndexNode rootMock = Mockito.mock(IndexNode.class);


        IndexNode node = IndexNode.createFromPath(rootMock, file);


        assertThat(node.getMode(), is(FileMode.FILE));
        assertThat(node.getName(), is("doe.txt"));
        assertThat(node.getSize(), is(4L));

        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
        assertThat(node.getCreationTime(), is(attributes.creationTime().toMillis()));
        assertThat(node.getModifiedTime(), is(attributes.lastModifiedTime().toMillis()));

        verifyInode(node.getInode(), attributes);
        assertThat(node.getHash(), is(FileHash.ZERO));

        assertThat(node.getChildren().isEmpty(), is(true));
    }

    @Test
    public void createFromPathOfDir() throws IOException {
        Path file = PathUtils.getResourcePath("index/foo");


        IndexNode node = IndexNode.createRootFromPath(file);


        assertThat(node.getMode(), is(FileMode.DIRECTORY));
        assertThat(node.getName(), is(""));
        assertThat(node.getSize(), is(not(0)));

        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
        assertThat(node.getCreationTime(), is(attributes.creationTime().toMillis()));
        assertThat(node.getModifiedTime(), is(attributes.lastModifiedTime().toMillis()));

        verifyInode(node.getInode(), attributes);
        assertThat(node.getHash(), is(FileHash.ZERO));

        assertThat(node.getChildren().isEmpty(), is(true));
    }

    @Test
    public void createFromPathShouldUpdateParent() throws IOException {
        Path file = PathUtils.getResourcePath("index/foo/doe.txt");
        IndexNode parentMock = Mockito.mock(IndexNode.class);


        IndexNode node = IndexNode.createFromPath(parentMock, file);


        assertThat(node.getParent(), is(parentMock));
    }

    @Test
    public void createFromIndex() {
        IndexNode node = IndexNode.createFromIndex(null, FileMode.DIRECTORY, 4096, 1474140125896L, 1481247415896L, 2285605, new FileHash("12345678901234567890".getBytes()), "foo");


        assertThat(node.getMode(), is(FileMode.DIRECTORY));
        assertThat(node.getName(), is("foo"));
        assertThat(node.getSize(), is(4096L));

        assertThat(node.getCreationTime(), is(1474140125896L));
        assertThat(node.getModifiedTime(), is(1481247415896L));

        assertThat(node.getInode(), is(2285605L));

        assertThat(node.getChildren().isEmpty(), is(true));
        assertThat(node.getHash().getBytes(), is("12345678901234567890".getBytes()));
    }

    @Test
    public void createFromIndexShouldUpdateParent() throws IOException {
        IndexNode parentMock = Mockito.mock(IndexNode.class);


        IndexNode node = IndexNode.createFromIndex(parentMock, FileMode.DIRECTORY, 4096, 1474140125896L, 1481247415896L, 2285605, new FileHash("12345678901234567890".getBytes()), "foo");


        assertThat(node.getParent(), is(parentMock));
    }

    @Test
    public void getRelativePathShouldCallParent() throws IOException {
        Path file = PathUtils.getResourcePath("index/foo/doe.txt");
        IndexNode parentMock = Mockito.mock(IndexNode.class);
        when(parentMock.getRelativePath()).thenReturn(Paths.get("index/bar"));
        IndexNode node = IndexNode.createFromPath(parentMock, file);


        Path path = node.getRelativePath();


        assertThat(path, is(Paths.get("index/bar/doe.txt")));
    }

    @Test
    public void resetHashesToRootNodeCallsParent() throws IOException {
        Path base = PathUtils.getResourcePath("index");
        IndexNode parentMock = Mockito.mock(IndexNode.class);

        IndexNode node = IndexNode.createFromPath(parentMock, base);


        node.resetHashesToRootNode();


        verify(parentMock, times(1)).resetHashesToRootNode();
    }

    @Test
    public void resetHashesToRootNodeRecalculatesHash() throws IOException {
        Path base = PathUtils.getResourcePath("index");

        IndexNode childMock = Mockito.mock(IndexNode.class);
        when(childMock.getHash()).thenReturn(new FileHash("12345678901234567890".getBytes()));
        when(childMock.getMode()).thenReturn(FileMode.FILE);
        when(childMock.getName()).thenReturn("foo.bar");

        IndexNode root = IndexNode.createRootFromPath(base);
        root.addChild(childMock);


        root.resetHashesToRootNode();
        FileHash hash = root.getHash();


        assertThat(HashUtil.toHex(hash.getBytes()), is("7e0953bdd7c8e3fbe607502407b336a696ab11fb"));
    }

    @Test
    public void addChildShouldRemoveChildWithSameName() throws IOException {
        Path base = PathUtils.getResourcePath("index");

        IndexNode root = IndexNode.createRootFromPath(base);
        givenChildrenNames(root, "foo.txt", "buz.txt", "readme.md", "doe.file");

        IndexNode childMock = createNodeMock("buz.txt");
        root.addChild(childMock);

        IndexNode result = root.findChildByName("buz.txt");


        assertThat(result, is(childMock));

        List<String> names = root.getChildren().stream().map(IndexNode::getName).sorted().collect(Collectors.toList());
        assertThat(names, is(Arrays.asList("buz.txt", "doe.file", "foo.txt", "readme.md")));
    }

    @Test
    public void findChildByName() throws IOException {
        Path base = PathUtils.getResourcePath("index");

        IndexNode root = IndexNode.createRootFromPath(base);
        givenChildrenNames(root, "foo.txt", "bar.txt", "readme.md");

        IndexNode childMock = createNodeMock("buz.txt");
        root.addChild(childMock);


        IndexNode result = root.findChildByName("buz.txt");


        assertThat(result, is(childMock));
    }

    private void givenChildrenNames(IndexNode root, String... names) {
        for (String name : names) {
            IndexNode nodeMock = createNodeMock(name);
            root.addChild(nodeMock);
        }
    }

    private IndexNode createNodeMock(String name) {
        IndexNode nodeMock = Mockito.mock(IndexNode.class);
        doReturn(name).when(nodeMock).getName();
        return nodeMock;
    }

    private void verifyInode(long inode, BasicFileAttributes attributes) {
        Object key = attributes.fileKey();
        if (key == null) {
            assertThat(inode, is(0));
        }

        String value = String.valueOf(key);
        Matcher matcher = Pattern.compile("ino=(\\d+)").matcher(value);
        if (matcher.find()) {
            long expected = Long.parseLong(matcher.group(1));
            assertThat(inode, is(expected));
        } else {
            assertThat(inode, is(0));
        }
    }
}