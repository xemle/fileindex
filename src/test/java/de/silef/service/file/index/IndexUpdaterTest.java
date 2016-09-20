package de.silef.service.file.index;

import de.silef.service.file.hash.FileHash;
import de.silef.service.file.util.HashUtil;
import de.silef.service.file.node.FileMode;
import de.silef.service.file.node.IndexNode;
import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by sebastian on 18.09.16.
 */
public class IndexUpdaterTest extends BasePathTest {

    private FileIndex index;

    private IndexUpdater updater;

    @Before
    public void setUp() throws IOException {
        super.setUp();
        PathUtils.copy(PathUtils.getResourcePath("index"), tmp);

        index = new FileIndex(tmp);
        index.initializeTreeHash();

        updater = new IndexUpdater(tmp, index.getRoot());
    }

    @Test
    public void updateShouldNotModifyRootHash() throws IOException {
        Set<IndexNode> nodes = index.getRoot()
                .stream()
                .filter(n -> n.getMode() == FileMode.FILE)
                .collect(Collectors.toSet());

        String existingRootHash = HashUtil.toHex(index.getRoot().getHash().getBytes());
        IndexChange change = new IndexChange(tmp, nodes, new HashSet<>(), new HashSet<>());


        updater.update(change, createHashConsumer(), true);


        String updatedRootHash = HashUtil.toHex(index.getRoot().getHash().getBytes());
        assertThat(updatedRootHash, is(existingRootHash));
    }

    @Test
    public void updateShouldInsertNewFile() throws IOException {
        givenFile("cats/funny/smile.txt", "content");


        updater.update(givenChange(), createHashConsumer(), true);


        String updatedRootHash = HashUtil.toHex(index.getRoot().getHash().getBytes());
        assertThat(updatedRootHash, is("df42942a34b3feea416126e39d0a2b7a1c6611e6"));
    }

    @Test
    public void updateShouldInsertchangedFile() throws IOException {
        givenFile("foo/doe.txt", "New Content");


        updater.update(givenChange(), createHashConsumer(), true);


        String updatedRootHash = HashUtil.toHex(index.getRoot().getHash().getBytes());
        assertThat(updatedRootHash, is("70b49ac758924e8771bfcbedcaaf8901d354666f"));
    }

    @Test
    public void updateShouldRemoveOldFile() throws IOException {
        Files.delete(tmp.resolve("foo/doe.txt"));


        updater.update(givenChange(), createHashConsumer(), true);


        String updatedRootHash = HashUtil.toHex(index.getRoot().getHash().getBytes());
        assertThat(updatedRootHash, is("fe2268f3a1950493a9d5dc1cb133640fc26696f3"));
    }

    private void givenFile(String path, String content) throws IOException {
        Path file = tmp.resolve(path);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes());
    }

    private IndexChange givenChange() throws IOException {
        FileIndex updatedIndex = new FileIndex(tmp);
        return IndexChange.create(tmp, updatedIndex.getRoot(), index.getRoot());
    }

    private Consumer<IndexNode> createHashConsumer() {
        return n -> {
            try {
                n.setHash(new FileHash(HashUtil.getHash(tmp.resolve(n.getRelativePath()))));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

}