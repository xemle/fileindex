package de.silef.service.file.util;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by sebastian on 18.09.16.
 */
public class ByteUtilTest {
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