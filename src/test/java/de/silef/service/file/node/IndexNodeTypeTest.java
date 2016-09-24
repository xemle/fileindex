package de.silef.service.file.node;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by sebastian on 24.09.16.
 */
public class IndexNodeTypeTest {

    @Test
    public void basic() {
        assertTrue(IndexNodeType.DIRECTORY.isDirectory());
        assertFalse(IndexNodeType.DIRECTORY.isFile());
        assertFalse(IndexNodeType.DIRECTORY.isLink());
        assertFalse(IndexNodeType.DIRECTORY.isOther());

        assertFalse(IndexNodeType.FILE.isDirectory());
        assertTrue(IndexNodeType.FILE.isFile());
        assertFalse(IndexNodeType.FILE.isLink());
        assertFalse(IndexNodeType.FILE.isOther());

        assertFalse(IndexNodeType.SYMLINK.isDirectory());
        assertFalse(IndexNodeType.SYMLINK.isFile());
        assertTrue(IndexNodeType.SYMLINK.isLink());
        assertFalse(IndexNodeType.SYMLINK.isOther());
    }
}