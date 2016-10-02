package de.silef.service.file.change;

import de.silef.service.file.extension.FileContentHashIndexExtension;
import de.silef.service.file.extension.IndexExtension;
import de.silef.service.file.index.FileIndex;
import de.silef.service.file.node.IndexNode;
import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

/**
 * Created by sebastian on 17.09.16.
 */
public class IndexChangeTest extends BasePathTest {

    @Test
    public void getChangeShouldHaveNewFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = FileIndex.create(tmp, indexStrategy);
        Files.write(tmp.resolve("new.txt"), "content".getBytes());

        FileIndex update = FileIndex.create(tmp, indexStrategy);


        IndexChange changes = old.getChanges(update, indexStrategy);


        assertThat(changes.hasChanges(), is(true));
        List<String> pathNames = changes.getCreated().stream().map(n -> n.getUpdate().getRelativePath().toString()).collect(Collectors.toList());
        assertThat(pathNames, is(Arrays.asList("new.txt")));
        assertThat(changes.getModified().isEmpty(), is(true));
        assertThat(changes.getRemoved().isEmpty(), is(true));
    }

    @Test
    public void getChangeShouldHaveModifiedFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = FileIndex.create(tmp, indexStrategy);
        Files.write(tmp.resolve("doe.txt"), "content".getBytes());

        FileIndex update = FileIndex.create(tmp, indexStrategy);


        IndexChange changes = update.getChanges(old, indexStrategy);


        List<String> pathNames = changes.getModified().stream().map(n -> n.getOrigin().getRelativePath().toString()).collect(Collectors.toList());
        assertThat(changes.hasChanges(), is(true));
        assertThat(changes.getCreated().isEmpty(), is(true));
        assertThat(pathNames, is(Arrays.asList("doe.txt")));
        assertThat(changes.getRemoved().isEmpty(), is(true));
    }

    @Test
    public void getChangeShouldHaveRemovedFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = FileIndex.create(tmp, indexStrategy);
        Files.delete(tmp.resolve("doe.txt"));

        FileIndex update = FileIndex.create(tmp, indexStrategy);


        IndexChange changes = old.getChanges(update, indexStrategy);


        List<String> pathNames = changes.getRemoved().stream().map(n -> n.getOrigin().getRelativePath().toString()).collect(Collectors.toList());
        assertThat(changes.hasChanges(), is(true));
        assertThat(changes.getCreated().isEmpty(), is(true));
        assertThat(changes.getModified().isEmpty(), is(true));
        assertThat(pathNames, is(Arrays.asList("doe.txt")));
    }

    @Test
    public void getChangeShouldWithFileTypeChangeFromFileToDir() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = FileIndex.create(tmp, indexStrategy);
        Files.delete(tmp.resolve("doe.txt"));
        Files.createDirectory(tmp.resolve("doe.txt"));

        FileIndex update = FileIndex.create(tmp, indexStrategy);


        IndexChange changes = update.getChanges(old, indexStrategy);


        List<String> createdPathNames = changes.getCreated().stream().map(n -> n.getUpdate().getRelativePath().toString()).collect(Collectors.toList());
        List<String> removedPathNames = changes.getRemoved().stream().map(n -> n.getOrigin().getRelativePath().toString()).collect(Collectors.toList());
        assertThat(changes.hasChanges(), is(true));
        assertThat(createdPathNames, is(Arrays.asList("doe.txt")));
        assertThat(changes.getModified().isEmpty(), is(true));
        assertThat(removedPathNames, is(Arrays.asList("doe.txt")));
    }

    @Test
    public void getChangeShouldWithFileTypeChangeFromDirToFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("index/foo"), tmp);
        FileIndex old = FileIndex.create(tmp, indexStrategy);
        PathUtils.delete(tmp.resolve("bar"));
        Files.write(tmp.resolve("bar"), "content".getBytes(), StandardOpenOption.CREATE_NEW);

        FileIndex update = FileIndex.create(tmp, indexStrategy);


        IndexChange changes = update.getChanges(old, indexStrategy);


        List<String> createdPathNames = changes.getCreated().stream().map(n -> n.getUpdate().getRelativePath().toString()).collect(Collectors.toList());
        List<String> removedPathNames = changes.getRemoved().stream().map(n -> n.getOrigin().getRelativePath().toString()).collect(Collectors.toList());
        assertThat(changes.hasChanges(), is(true));
        assertThat(createdPathNames, is(Arrays.asList("bar")));
        assertThat(changes.getModified().isEmpty(), is(true));
        assertThat(removedPathNames, is(Arrays.asList("bar")));
    }

    @Test
    public void applyCreated() throws IOException {
        FileIndex old = FileIndex.create(tmp, indexStrategy);
        Files.write(tmp.resolve("new.txt"), "content".getBytes());

        FileIndex update = FileIndex.create(tmp, indexStrategy);
        IndexChange changes = old.getChanges(update, indexStrategy);


        changes.apply();


        assertThat(old.getRoot().getChildByName("new.txt"), is(not(nullValue())));
    }

    @Test
    public void applyModified() throws IOException {
        Files.write(tmp.resolve("foo.txt"), "content".getBytes());
        FileIndex old = FileIndex.create(tmp, indexStrategy);

        Files.write(tmp.resolve("foo.txt"), "update".getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        FileIndex update = FileIndex.create(tmp, indexStrategy);

        IndexChange changes = old.getChanges(update, indexStrategy);

        IndexNode updateFooTxt = update.getRoot().getChildByName("foo.txt");


        changes.apply();


        assertThat(old.getRoot().getChildByName("foo.txt"), is(not(nullValue())));
        IndexNode oldFooTxt = old.getRoot().getChildByName("foo.txt");
        verifyExtensions(oldFooTxt, updateFooTxt.getExtensions());
    }

    @Test
    public void applyModifiedShouldRemoveExistingExtensions() throws IOException {
        Files.write(tmp.resolve("foo.txt"), "content".getBytes());
        FileIndex old = FileIndex.create(tmp, indexStrategy);
        IndexNode foo = old.getRoot().getChildByName("foo.txt");
        foo.addExtension(FileContentHashIndexExtension.create(tmp.resolve("foo.txt")));

        Files.write(tmp.resolve("foo.txt"), "update".getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        FileIndex update = FileIndex.create(tmp, indexStrategy);

        IndexChange changes = old.getChanges(update, indexStrategy);

        IndexNode updateFooTxt = update.getRoot().getChildByName("foo.txt");


        changes.apply();


        assertThat(foo, is(not(nullValue())));
        IndexNode oldFooTxt = old.getRoot().getChildByName("foo.txt");
        verifyExtensions(oldFooTxt, updateFooTxt.getExtensions());
    }

    private void verifyExtensions(IndexNode node, List<IndexExtension> extensions) {
        for (IndexExtension extension : extensions) {
            IndexExtension nodeExtension = node.getExtensionByType(extension.getType());
            assertThat(nodeExtension, is(not(nullValue())));
            assertThat(nodeExtension.getData(), is(extension.getData()));
        }
        assertThat(node.getExtensions().size(), is(extensions.size()));
    }

    @Test
    public void applyRemoved() throws IOException {
        Files.write(tmp.resolve("foo.txt"), "content".getBytes());
        FileIndex old = FileIndex.create(tmp, indexStrategy);

        Files.delete(tmp.resolve("foo.txt"));
        FileIndex update = FileIndex.create(tmp, indexStrategy);
        IndexChange changes = old.getChanges(update, indexStrategy);


        changes.apply();


        assertThat(update.getRoot().getChildByName("foo.txt"), is(nullValue()));
    }

}