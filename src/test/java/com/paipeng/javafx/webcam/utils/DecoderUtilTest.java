package com.paipeng.javafx.webcam.utils;

import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeParam;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeScore;
import com.s2icode.jna.utils.ImageUtils;
import junit.framework.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;
class DecoderUtilTest {
    public static Logger logger = LoggerFactory.getLogger(DecoderUtilTest.class);

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void doDecodeWithDetect() {
        String file = "/Users/paipeng/Downloads/3f0b0b2f-f866-4a1f-bf78-c85f85f2d33a.jpeg";
        BufferedImage bufferedImage = ImageUtils.fileToBufferedImage(file);
        Assert.assertNotNull(bufferedImage);
        DecoderUtil.getInstance().initNanogridDecoder();
        DecoderUtil.getInstance().doDecodeWithDetect(bufferedImage, new DecoderUtil.DecodeUtilInterface() {
            @Override
            public void decodedSuccess(S2iDecodeParam.ByReference s2iDecodeParam, S2iDecodeScore.ByReference s2iDecodeScore) throws UnsupportedEncodingException {
                logger.trace("decode result: " + s2iDecodeParam);
            }
        });
    }
}