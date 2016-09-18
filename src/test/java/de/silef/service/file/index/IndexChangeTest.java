package de.silef.service.file.index;

import de.silef.service.file.hash.FileHash;
import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexChangeTest extends BasePathTest {

    @Test
    public void getChangeShouldHaveNewFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = new FileIndex(tmp);
        Files.write(tmp.resolve("new.txt"), "content".getBytes());

        FileIndex update = new FileIndex(tmp);


        IndexChange changes = update.getChanges(old);


        assertThat(changes.hasChanges(), is(true));
        List<String> pathNames = changes.getCreated().stream().map(n -> n.getRelativePath().toString()).collect(Collectors.toList());
        assertThat(pathNames, is(Arrays.asList("new.txt")));
        assertThat(changes.getModified().isEmpty(), is(true));
        assertThat(changes.getRemoved().isEmpty(), is(true));
    }

    @Test
    public void getChangeShouldHaveModifiedFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = new FileIndex(tmp);
        Files.write(tmp.resolve("doe.txt"), "content".getBytes());

        FileIndex update = new FileIndex(tmp);


        IndexChange changes = update.getChanges(old);


        List<String> pathNames = changes.getModified().stream().map(n -> n.getRelativePath().toString()).collect(Collectors.toList());
        assertThat(changes.hasChanges(), is(true));
        assertThat(changes.getCreated().isEmpty(), is(true));
        assertThat(pathNames, is(Arrays.asList("doe.txt")));
        assertThat(changes.getRemoved().isEmpty(), is(true));
    }

    @Test
    public void getChangeShouldHaveRemovedFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = new FileIndex(tmp);
        Files.delete(tmp.resolve("doe.txt"));

        FileIndex update = new FileIndex(tmp);


        IndexChange changes = update.getChanges(old);


        List<String> pathNames = changes.getRemoved().stream().map(n -> n.getRelativePath().toString()).collect(Collectors.toList());
        assertThat(changes.hasChanges(), is(true));
        assertThat(changes.getCreated().isEmpty(), is(true));
        assertThat(changes.getModified().isEmpty(), is(true));
        assertThat(pathNames, is(Arrays.asList("doe.txt")));
    }

    @Test
    public void copyKnownHashesFromOldToNewIndex() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = new FileIndex(tmp);

        old.initializeTreeHash();

        FileIndex update = new FileIndex(tmp);


        update.getChanges(old);


        IndexNode doe = update.getRoot().stream().filter(n -> n.getName().equals("doe.txt")).findFirst().get();
        assertThat(doe.getHash().equals(FileHash.ZERO), is(false));
    }
}