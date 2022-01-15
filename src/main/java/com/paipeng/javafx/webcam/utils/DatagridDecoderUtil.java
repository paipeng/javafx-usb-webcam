package com.paipeng.javafx.webcam.utils;

import com.s2icode.jna.datagrid.decoder.S2iDatagridDecoder;
import com.s2icode.jna.nanogrid.decoder.S2iNanogridDecoder;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeInfo;
import com.s2icode.jna.s2idetect.S2iDetect;
import com.sun.jna.Pointer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import static com.s2icode.jna.utils.ImageUtils.convertPointerToBufferedImage;

public class DatagridDecoderUtil extends DecoderUtil {
    private S2iDatagridDecoder s2iDatagridDecoder;

    private static DatagridDecoderUtil instance;

    public static DatagridDecoderUtil getInstance() {
        if (instance == null)
            instance = new DatagridDecoderUtil();
        return instance;
    }

    public void initDecoder() {
        //index++;
        ArrayList<String> searchPathList = new ArrayList<String>();
        searchPathList.add(System.getProperty("user.dir") + "/libs");
        //searchPathList.add(System.getProperty("user.home") + "/" + WebCamAppLauncher.APPLICATION_CONFIG_FOLDER + "/libs");
        logger.info("initDecoder lib解码路径：++++++++++++++++++++++++++" + searchPathList.get(0));
        s2iDatagridDecoder = new S2iDatagridDecoder(searchPathList, false);

        s2iDetect = new S2iDetect(searchPathList, false);
    }

    @Override
    protected BufferedImage getSlaviDetectedBufferedImage(Pointer ex8p) {
        BufferedImage bufferedImage = convertPointerToBufferedImage(ex8p, 320, 320);
        return bufferedImage;
    }

    @Override
    public void doDecodeWithDetect(BufferedImage bufferedImage, DecodeUtilInterface decodeUtilInterface) {
        logger.trace("doDecodeWithDetect");
    }
}
