package de.silef.service.file.extension;

import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.test.PathUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by sebastian on 25.09.16.
 */
public class BasicFileIndexExtensionTest extends BasePathTest{

    @Test
    public void createFromPathShouldHaveSameCreationtime() throws IOException, InterruptedException {
        Path base = PathUtils.getResourcePath("index/foo");

        BasicFileIndexExtension extension1 = BasicFileIndexExtension.createFromPath(base);


        Thread.sleep(50);
        BasicFileIndexExtension extension2 = BasicFileIndexExtension.createFromPath(base);


        assertThat(extension2.getCreationTime(), is(extension1.getCreationTime()));
        assertThat(extension2.getModifiedTime(), is(extension1.getModifiedTime()));
    }

    @Test
    public void createFromPathShouldChangeTimestampsAfterFileCreation() throws IOException, InterruptedException {
        Path base = PathUtils.getResourcePath("index/foo");
        PathUtils.copy(base, tmp);

        BasicFileIndexExtension extension1 = BasicFileIndexExtension.createFromPath(tmp);


        Thread.sleep(50);
        Files.write(tmp.resolve("foo.txt"), "content".getBytes());


        BasicFileIndexExtension extension2 = BasicFileIndexExtension.createFromPath(tmp);

        assertNotEquals(extension2.getCreationTime(), extension1.getCreationTime());
        assertNotEquals(extension2.getModifiedTime(), extension1.getModifiedTime());
    }

    @Test
    public void createFromPathShouldNotChangeCreationTimeAfterFileCreationInSubDirectory() throws IOException, InterruptedException {
        Path base = PathUtils.getResourcePath("index/foo");
        PathUtils.copy(base, tmp);

        BasicFileIndexExtension extension1 = BasicFileIndexExtension.createFromPath(tmp);


        Thread.sleep(50);
        Files.write(tmp.resolve("bar/foo.txt"), "content".getBytes());


        BasicFileIndexExtension extension2 = BasicFileIndexExtension.createFromPath(tmp);

        assertThat(extension2.getCreationTime(), is(extension1.getCreationTime()));
        assertThat(extension2.getModifiedTime(), is(extension1.getModifiedTime()));
    }
}