package de.silef.service.file.util;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by sebastian on 18.09.16.
 */
public class ByteUtilTest {

    @Test
    public void toHumanSize() {
        assertThat(ByteUtil.toHumanSize(5L), is("5B"));
        assertThat(ByteUtil.toHumanSize(1048576L), is("1024.0KB"));
        assertThat(ByteUtil.toHumanSize(214748364L), is("204.8MB"));
        assertThat(ByteUtil.toHumanSize(12433741824L), is("11.6GB"));
    }

    @Test
    public void toByte() throws Exception {
        assertThat(ByteUtil.toByte("5M"), is(5242880L));
        assertThat(ByteUtil.toByte("1024"), is(1024L));
        assertThat(ByteUtil.toByte("1024b"), is(1024L));
        assertThat(ByteUtil.toByte("1024k"), is(1048576L));
        assertThat(ByteUtil.toByte("1024mb"), is(1073741824L));
        assertThat(ByteUtil.toByte("0.2gB"), is(214748364L));
    }

}