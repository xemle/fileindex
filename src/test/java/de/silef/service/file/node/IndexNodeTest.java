package de.silef.service.file.node;

import de.silef.service.file.test.BaseTest;
import de.silef.service.file.test.PathUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by sebastian on 18.09.16.
 */
public class IndexNodeTest extends BaseTest {

    @Test
    public void createFromPathOfFile() throws IOException {
        Path file = PathUtils.getResourcePath("index/foo/doe.txt");
        IndexNode rootMock = Mockito.mock(IndexNode.class);


        IndexNode node = IndexNode.createFromPath(rootMock, file);


        assertThat(node.getNodeType().equals(IndexNodeType.FILE), is(true));
        assertThat(node.getName(), is("doe.txt"));

        assertThat(node.getChildren().isEmpty(), is(true));
    }

    @Test
    public void createFromPathOfDir() throws IOException {
        Path file = PathUtils.getResourcePath("index/foo");


        IndexNode node = IndexNode.createFromPath(null, file);


        assertThat(node.getNodeType().equals(IndexNodeType.DIRECTORY), is(true));
        assertThat(node.getName(), is(""));

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
    public void getRelativePathShouldCallParent() throws IOException {
        Path file = PathUtils.getResourcePath("index/foo/doe.txt");
        IndexNode parentMock = Mockito.mock(IndexNode.class);
        when(parentMock.getRelativePath()).thenReturn(Paths.get("index/bar"));
        IndexNode node = IndexNode.createFromPath(parentMock, file);


        Path path = node.getRelativePath();


        assertThat(path, is(Paths.get("index/bar/doe.txt")));
    }

    @Test
    public void addChildShouldRemoveChildWithSameName() throws IOException {
        Path base = PathUtils.getResourcePath("index");

        IndexNode root = IndexNode.createFromPath(null, base);
        givenChildrenNames(root, "foo.txt", "buz.txt", "readme.md", "doe.file");

        IndexNode childMock = createNodeMock("buz.txt");
        root.addChild(childMock);

        IndexNode result = root.getChildByName("buz.txt");


        assertThat(result, is(childMock));

        List<String> names = root.getChildren().stream().map(IndexNode::getName).sorted().collect(Collectors.toList());
        assertThat(names, is(Arrays.asList("buz.txt", "doe.file", "foo.txt", "readme.md")));
    }

    @Test
    public void findChildByName() throws IOException {
        Path base = PathUtils.getResourcePath("index");

        IndexNode root = standardFileIndexStrategy.createFromPath(null, base);
        givenChildrenNames(root, "foo.txt", "bar.txt", "readme.md");

        IndexNode childMock = createNodeMock("buz.txt");
        root.addChild(childMock);


        IndexNode result = root.getChildByName("buz.txt");


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

}