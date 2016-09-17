package de.silef.service.file.meta;

import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by sebastian on 17.09.16.
 */
public class FileMetaNodeCacheTest extends BasePathTest {

    @Test
    public void getChangeShouldHaveNewFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("meta/foo"), tmp);
        FileMetaCache old = new FileMetaCache(tmp);
        Files.write(tmp.resolve("new.txt"), "content".getBytes());

        FileMetaCache update = new FileMetaCache(tmp);


        FileMetaChanges changes = update.getChanges(old);


        assertThat(changes.hasChanges(), is(true));
        assertThat(changes.getCreated(), is(Arrays.asList("new.txt")));
        assertThat(changes.getModified().isEmpty(), is(true));
        assertThat(changes.getRemoved().isEmpty(), is(true));
    }

    @Test
    public void getChangeShouldHaveModifiedFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("meta/foo"), tmp);
        FileMetaCache old = new FileMetaCache(tmp);
        Files.write(tmp.resolve("doe.txt"), "content".getBytes());

        FileMetaCache update = new FileMetaCache(tmp);


        FileMetaChanges changes = update.getChanges(old);


        assertThat(changes.hasChanges(), is(true));
        assertThat(changes.getCreated().isEmpty(), is(true));
        assertThat(changes.getModified(), is(Arrays.asList("doe.txt")));
        assertThat(changes.getRemoved().isEmpty(), is(true));
    }

    @Test
    public void getChangeShouldHaveRemovedFile() throws IOException {
        PathUtils.copy(PathUtils.getResourcePath("meta/foo"), tmp);
        FileMetaCache old = new FileMetaCache(tmp);
        Files.delete(tmp.resolve("doe.txt"));

        FileMetaCache update = new FileMetaCache(tmp);


        FileMetaChanges changes = update.getChanges(old);


        assertThat(changes.hasChanges(), is(true));
        assertThat(changes.getCreated().isEmpty(), is(true));
        assertThat(changes.getModified().isEmpty(), is(true));
        assertThat(changes.getRemoved(), is(Arrays.asList("doe.txt")));
    }
}