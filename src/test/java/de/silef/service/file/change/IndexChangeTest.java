package de.silef.service.file.change;

import de.silef.service.file.index.FileIndex;
import de.silef.service.file.change.IndexChange;
import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexChangeTest extends BasePathTest {

    @Test
    public void getChangeShouldHaveNewFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = FileIndex.create(tmp, standardFileIndexStrategy);
        Files.write(tmp.resolve("new.txt"), "content".getBytes());

        FileIndex update = FileIndex.create(tmp, standardFileIndexStrategy);


        IndexChange changes = update.getChanges(old, standardFileIndexStrategy);


        assertThat(changes.hasChanges(), is(true));
        List<String> pathNames = changes.getCreated().stream().map(n -> n.getOther().getRelativePath().toString()).collect(Collectors.toList());
        assertThat(pathNames, is(Arrays.asList("new.txt")));
        assertThat(changes.getModified().isEmpty(), is(true));
        assertThat(changes.getRemoved().isEmpty(), is(true));
    }

    @Test
    public void getChangeShouldHaveModifiedFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = FileIndex.create(tmp, standardFileIndexStrategy);
        Files.write(tmp.resolve("doe.txt"), "content".getBytes());

        FileIndex update = FileIndex.create(tmp, standardFileIndexStrategy);


        IndexChange changes = update.getChanges(old, standardFileIndexStrategy);


        List<String> pathNames = changes.getModified().stream().map(n -> n.getPrimary().getRelativePath().toString()).collect(Collectors.toList());
        assertThat(changes.hasChanges(), is(true));
        assertThat(changes.getCreated().isEmpty(), is(true));
        assertThat(pathNames, is(Arrays.asList("doe.txt")));
        assertThat(changes.getRemoved().isEmpty(), is(true));
    }

    @Test
    public void getChangeShouldHaveRemovedFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = FileIndex.create(tmp, standardFileIndexStrategy);
        Files.delete(tmp.resolve("doe.txt"));

        FileIndex update = FileIndex.create(tmp, standardFileIndexStrategy);


        IndexChange changes = update.getChanges(old, standardFileIndexStrategy);


        List<String> pathNames = changes.getRemoved().stream().map(n -> n.getPrimary().getRelativePath().toString()).collect(Collectors.toList());
        assertThat(changes.hasChanges(), is(true));
        assertThat(changes.getCreated().isEmpty(), is(true));
        assertThat(changes.getModified().isEmpty(), is(true));
        assertThat(pathNames, is(Arrays.asList("doe.txt")));
    }

    @Test
    public void getChangeShouldWithFileTypeChangeFromFileToDir() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = FileIndex.create(tmp, standardFileIndexStrategy);
        Files.delete(tmp.resolve("doe.txt"));
        Files.createDirectory(tmp.resolve("doe.txt"));

        FileIndex update = FileIndex.create(tmp, standardFileIndexStrategy);


        IndexChange changes = update.getChanges(old, standardFileIndexStrategy);


        List<String> createdPathNames = changes.getCreated().stream().map(n -> n.getOther().getRelativePath().toString()).collect(Collectors.toList());
        List<String> removedPathNames = changes.getRemoved().stream().map(n -> n.getPrimary().getRelativePath().toString()).collect(Collectors.toList());
        assertThat(changes.hasChanges(), is(true));
        assertThat(createdPathNames, is(Arrays.asList("doe.txt")));
        assertThat(changes.getModified().isEmpty(), is(true));
        assertThat(removedPathNames, is(Arrays.asList("doe.txt")));
    }

    @Test
    public void getChangeShouldWithFileTypeChangeFromDirToFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = FileIndex.create(tmp, standardFileIndexStrategy);
        PathUtils.delete(tmp.resolve("bar"));
        Files.write(tmp.resolve("bar"), "content".getBytes(), StandardOpenOption.CREATE_NEW);

        FileIndex update = FileIndex.create(tmp, standardFileIndexStrategy);


        IndexChange changes = update.getChanges(old, standardFileIndexStrategy);


        List<String> createdPathNames = changes.getCreated().stream().map(n -> n.getOther().getRelativePath().toString()).collect(Collectors.toList());
        List<String> removedPathNames = changes.getRemoved().stream().map(n -> n.getPrimary().getRelativePath().toString()).collect(Collectors.toList());
        assertThat(changes.hasChanges(), is(true));
        assertThat(createdPathNames, is(Arrays.asList("bar")));
        assertThat(changes.getModified().isEmpty(), is(true));
        assertThat(removedPathNames, is(Arrays.asList("bar")));
    }

}