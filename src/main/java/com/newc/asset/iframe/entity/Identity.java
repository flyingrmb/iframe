package com.newc.asset.iframe.entity;

import com.newc.asset.iframe.util.Reflection;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * The abstraction of Identity, one Identity identify one unique Any Object.
 * Created by paul on 2018/5/10.
 */
public interface Identity<T> {
    boolean dimensionAs(Identity another);

    Identity<Entry> zip(Identity right);

    Identity unzip(ZipSide side);

    boolean matches(Object object);

    Identity values(Object object);
}
