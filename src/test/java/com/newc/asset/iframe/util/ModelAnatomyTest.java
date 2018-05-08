package com.newc.asset.iframe.util;

import com.newc.asset.iframe.entity.Model;
import com.newc.asset.itest.annotation.TestData;
import com.newc.asset.itest.data.TestDataLoader;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by paul on 2018/5/8.
 */
@TestData("classpath:data/model.json")
public class ModelAnatomyTest {
    @Test
    @TestData(node = "simple_model")
    public void simpleDissect() throws IOException {
        Model model = TestDataLoader.loadTestData(Model.class);
    }
}