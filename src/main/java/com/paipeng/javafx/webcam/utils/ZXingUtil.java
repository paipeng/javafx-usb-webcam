package com.paipeng.javafx.webcam.utils;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.Hashtable;

public class ZXingUtil {
    public static Logger logger = LoggerFactory.getLogger(DecoderUtil.class);
    public static String qrCodeDecode(BufferedImage bufferedImage) {

        String resultStr = null;
        if (bufferedImage == null) {
            logger.error("bufferedImage is null.");
            return resultStr;
        }

        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Hashtable hints = new Hashtable();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        try {
            Result result = new MultiFormatReader().decode(bitmap, hints);
            resultStr = result.getText();
        } catch (NotFoundException e) {
            logger.error("qr decode error:" + e.getMessage());
        }
        return resultStr;
    }
}
