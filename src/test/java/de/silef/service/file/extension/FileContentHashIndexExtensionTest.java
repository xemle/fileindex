package de.silef.service.file.extension;

import de.silef.service.file.test.BasePathTest;
import de.silef.service.file.util.HashUtil;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by sebastian on 02.10.16.
 */
public class FileContentHashIndexExtensionTest extends BasePathTest {

    @Test
    public void createOfBrokenLinkShouldNotFail() throws Exception {
        Path link = tmp.resolve("broken.link");
        Files.createSymbolicLink(link, Paths.get("vanished.file"));


        IndexExtension extension = FileContentHashIndexExtension.create(link);


        // SHA1(0x0000000000000000 + "vanished.file")
        assertThat(HashUtil.toHex(extension.getData()), is("9e027c385d55371f7bb241f54c94559b9aad7b3a"));
    }

}