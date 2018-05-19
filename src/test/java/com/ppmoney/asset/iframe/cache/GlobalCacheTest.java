package com.ppmoney.asset.iframe.cache;

import com.ppmoney.asset.iframe.entity.Identity;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

/**
 * Created by paul on 2018/5/10.
 */
public class GlobalCacheTest {
    @Test
    public void cacheObject() {
        GlobalCache cache = GlobalCache.getInstance();
        Identity identity = cache.put(new Integer(1));

        int intv = cache.get(identity, Integer.class);
        assertThat(intv, is(1));

        cache.remove(identity);

        Integer intv2 = cache.get(identity, Integer.class);
        assertThat(intv2, is(nullValue()));
    }
}