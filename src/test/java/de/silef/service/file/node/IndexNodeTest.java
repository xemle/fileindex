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

import static org.hamcrest.CoreMatchers.nullValue;
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


        IndexNode node = createFromPath(rootMock, file);


        assertThat(node.getNodeType().equals(IndexNodeType.FILE), is(true));
        assertThat(node.getName(), is("doe.txt"));

        assertThat(node.getChildren().isEmpty(), is(true));
    }

    @Test
    public void createFromPathOfDir() throws IOException {
        Path file = PathUtils.getResourcePath("index/foo");


        IndexNode node = createFromPath(null, file);


        assertThat(node.getNodeType().equals(IndexNodeType.DIRECTORY), is(true));
        assertThat(node.getName(), is(""));

        assertThat(node.getChildren().isEmpty(), is(true));
    }

    @Test
    public void createFromPathShouldUpdateParent() throws IOException {
        Path file = PathUtils.getResourcePath("index/foo/doe.txt");
        IndexNode parentMock = Mockito.mock(IndexNode.class);


        IndexNode node = createFromPath(parentMock, file);


        assertThat(node.getParent(), is(parentMock));
    }

    @Test
    public void getRelativePathShouldCallParent() throws IOException {
        Path file = PathUtils.getResourcePath("index/foo/doe.txt");
        IndexNode parentMock = Mockito.mock(IndexNode.class);
        when(parentMock.getRelativePath()).thenReturn(Paths.get("index/bar"));
        IndexNode node = createFromPath(parentMock, file);


        Path path = node.getRelativePath();


        assertThat(path, is(Paths.get("index/bar/doe.txt")));
    }

    @Test
    public void addChildShouldRemoveChildWithSameName() throws IOException {
        Path base = PathUtils.getResourcePath("index");

        IndexNode root = createFromPath(null, base);
        givenChildrenNames(root, "foo.txt", "buz.txt", "readme.md", "doe.file");

        IndexNode childMock = createNodeMock(null, "buz.txt");
        root.addChild(childMock);

        IndexNode result = root.getChildByName("buz.txt");


        assertThat(result, is(childMock));

        List<String> names = root.getChildren().stream().map(IndexNode::getName).sorted().collect(Collectors.toList());
        assertThat(names, is(Arrays.asList("buz.txt", "doe.file", "foo.txt", "readme.md")));
    }

    @Test
    public void addChildShouldSetNewParent() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        IndexNode rootA = createFromPath(null, base);
        IndexNode rootB = createFromPath(null, base);
        IndexNode nodeB = createFromPath(rootB, base.resolve("doe.txt"));
        rootB.setChildren(Arrays.asList(nodeB));


        rootA.addChild(nodeB);


        assertThat(nodeB.getParent(), is(rootA));
    }

    @Test
    public void addChildShouldRemoveExistingNode() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        IndexNode rootA = createFromPath(null, base);
        IndexNode nodeA = createFromPath(rootA, base.resolve("doe.txt"));
        rootA.setChildren(Arrays.asList(nodeA));
        IndexNode rootB = createFromPath(null, base);
        IndexNode nodeB = createFromPath(rootB, base.resolve("doe.txt"));
        rootB.setChildren(Arrays.asList(nodeB));


        rootA.addChild(nodeB);


        assertThat(rootA.getChildren().size(), is(1));
        assertThat(rootA.getChildByName("doe.txt"), is(nodeB));
        assertThat(nodeA.getParent(), is(nullValue()));
    }

    @Test
    public void addChildShouldRemoveNodeFromOldParent() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        IndexNode rootA = createFromPath(null, base);
        IndexNode rootB = createFromPath(null, base);
        IndexNode nodeB = createFromPath(rootB, base.resolve("doe.txt"));
        rootB.setChildren(Arrays.asList(nodeB));


        rootA.addChild(nodeB);


        assertThat(rootB.getChildByName("doe.txt"), is(nullValue()));
    }

    @Test
    public void addChildShouldResetRelativePath() throws IOException {
        Path base = PathUtils.getResourcePath("index/foo");
        IndexNode rootA = createFromPath(null, base);
        IndexNode rootB = createFromPath(null, base);
        IndexNode dirB = createFromPath(rootB, base.resolve("bar"));
        IndexNode nodeB = createFromPath(dirB, base.resolve("zoo.txt"));
        dirB.setChildren(Arrays.asList(nodeB));
        rootB.setChildren(Arrays.asList(dirB));

        Path oldRelativePath = nodeB.getRelativePath();


        rootA.addChild(nodeB);


        assertThat(oldRelativePath, is(Paths.get("bar/zoo.txt")));
        assertThat(nodeB.getRelativePath(), is(Paths.get("zoo.txt")));
    }

    @Test
    public void findChildByName() throws IOException {
        Path base = PathUtils.getResourcePath("index");

        IndexNode root = indexStrategy.createIndexNode(null, indexStrategy.createPathInfo(base));
        givenChildrenNames(root, "foo.txt", "bar.txt", "readme.md");

        IndexNode childMock = createNodeMock(null, "buz.txt");
        root.addChild(childMock);


        IndexNode result = root.getChildByName("buz.txt");


        assertThat(result, is(childMock));
    }

    private IndexNode createFromPath(IndexNode parent, Path path) throws IOException {
        IndexNodeType nodeType = IndexNodeType.create(path);
        boolean isRootNode = parent == null;
        String name = isRootNode ? "" : path.getFileName().toString();
        return new IndexNode(parent, nodeType, name);
    }

    private void givenChildrenNames(IndexNode parent, String... names) {
        for (String name : names) {
            IndexNode nodeMock = createNodeMock(parent, name);
            parent.addChild(nodeMock);
        }
    }

    private IndexNode createNodeMock(IndexNode parent, String name) {
        IndexNode nodeMock = Mockito.mock(IndexNode.class);
        doReturn(parent).when(nodeMock).getParent();
        doReturn(name).when(nodeMock).getName();
        return nodeMock;
    }

}