package com.paipeng.javafx.webcam.utils;

import com.s2icode.jna.model.S2iCodeImage;
import com.s2icode.jna.nanogrid.decoder.S2iNanogridDecoder;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeConfig;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeInfo;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeParam;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeScore;
import com.s2icode.jna.s2idetect.S2iDetect;
import com.s2icode.jna.s2idetect.model.S2iDetectParam;
import com.s2icode.jna.s2idetect.model.SlaviDetectParam;
import com.s2icode.jna.s2idetect.model.SlaviDetectResult;
import com.s2icode.jna.utils.ImageUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

import static com.s2icode.jna.utils.ImageUtils.convertPointerToBufferedImage;

public class DecoderUtil {
    public static Logger logger = LoggerFactory.getLogger(DecoderUtil.class);

    private S2iDetect s2iDetect = null;
    private S2iNanogridDecoder s2iNanogridDecoder;
    //通过bufferedimage转换成所需要的图片格式
    private static S2iCodeImage.ByReference s2iCodeImageModel = null;

    //解码，防伪码基本信息
    private static S2iDecodeParam.ByReference decodeParam = new S2iDecodeParam.ByReference();
    //定义扩增数据
    private static S2iDecodeConfig.ByReference decodeConfig = new S2iDecodeConfig.ByReference();
    //解码，防伪码的扩展信息（纳米纹理）
    private static S2iDecodeScore.ByReference decodeScore = new S2iDecodeScore.ByReference();
    //图片识别数据（in）
    private static S2iDetectParam.ByValue s2iDetectParam = new S2iDetectParam.ByValue();

    private static Pointer ex8p = null;

    private static DecoderUtil instance;

    public static DecoderUtil getInstance() {
        if (instance == null)
            instance = new DecoderUtil();
        return instance;
    }

    public void initNanogridDecoder() {
        //index++;
        ArrayList<String> searchPathList = new ArrayList<String>();
        searchPathList.add(System.getProperty("user.dir") + "/libs");
        //searchPathList.add(System.getProperty("user.home") + "/" + WebCamAppLauncher.APPLICATION_CONFIG_FOLDER + "/libs");
        logger.info("initDecoder lib解码路径：++++++++++++++++++++++++++" + searchPathList.get(0));
        s2iNanogridDecoder = new S2iNanogridDecoder(searchPathList, false);
        S2iDecodeInfo.ByReference decodeInfo = new S2iDecodeInfo.ByReference();
        if (s2iNanogridDecoder.s2i_nanogrid_decode_init(decodeInfo) == 0) {

        }
        s2iDetect = new S2iDetect(searchPathList, false);
    }

    public void doDecodeWithDetect(BufferedImage bufferedImage, DecodeUtilInterface decodeUtilInterface) {
        logger.trace("doDecodeWithDetect");
        S2iDecodeScore.ByReference decodeScore = new S2iDecodeScore.ByReference();
        //通过bufferedimage转换成所需要的图片格式
        S2iCodeImage.ByReference s2iCodeImageModel = new S2iCodeImage.ByReference();

        ex8p = new Memory(640 * 640 * Native.getNativeSize(Byte.TYPE));
        logger.info("Pointer size" + ex8p.SIZE + "-" + ex8p.toString());

        decodeConfig.calculate_epic = 0;
        decodeConfig.check_nano = 1;//获取纳米数据 0 否  1是

        Arrays.fill(decodeScore.epic_data, (byte) 0);
        decodeScore.epicValidate = 0;

        //调用解码函数 0 是解码成功
        int ret = 10;
        try {
            BufferedImage detectedBufferedImage = slaviDetectBufferedImage(bufferedImage);
            byte[] tmp = ImageUtils.convertBufferedImageToBytes(detectedBufferedImage);
            Pointer pointer = new Memory(tmp.length);
            for (int i = 0; i < tmp.length; i++) {
                byte tmpb = tmp[i];
                pointer.setByte(i, tmpb);
            }
            s2iCodeImageModel.setDataPointer(pointer);
            s2iCodeImageModel.width = 640;
            s2iCodeImageModel.height = 640;
            ret = s2iNanogridDecoder.decode(s2iCodeImageModel, decodeConfig, decodeParam, decodeScore);
            logger.trace("decode ret: " + ret + " score: " + decodeScore.imageQuality + " nano: " + decodeScore.nanoGridCoefficient);
            logger.trace("decodeParam brand owner name: " + new String(decodeParam.brand_owner_name, 0, 30, "GB18030"));
            logger.trace("decodeParam clientId: " + decodeParam.client_id);
            logger.trace("decodeParam serial_number: " + decodeParam.serial_number);
            logger.trace("decodeParam productId: " + decodeParam.product_id);
            logger.trace("decodeParam data: " + new String(decodeParam.data, 0, decodeParam.data_len, "GB18030"));


            if (ret == 1 || ret == -5002) {
                decodeUtilInterface.decodedSuccess(decodeParam, decodeScore);
            } else {
                decodeUtilInterface.decodedSuccess(null, null);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    public SlaviDetectParam.ByReference getSlaviDetectParam() {
        SlaviDetectParam.ByReference slaviDetectParam = new SlaviDetectParam.ByReference();

        slaviDetectParam.crop_width = 640;
        slaviDetectParam.crop_height = 640;
        slaviDetectParam.image_rotate = 0;
        slaviDetectParam.border_size = 72;

        slaviDetectParam.max_brightness = 210;
        slaviDetectParam.min_brightness = 50;
        slaviDetectParam.min_dist_bw = 10;
        slaviDetectParam.max_rescale_factor = 1.04f;
        slaviDetectParam.min_rescale_factor = 0.96f;
        slaviDetectParam.min_sharpness = 10;
        slaviDetectParam.min_mseq_intensitive = 5;
        slaviDetectParam.min_mseq_snr = 10;
        slaviDetectParam.resize_width = 640;
        slaviDetectParam.resize_height = 640;
        slaviDetectParam.improve_histogram = 0;
        slaviDetectParam.detect_rotate = 0;
        slaviDetectParam.rescale_factor = 0.01f;
        slaviDetectParam.color_channel = 0;
        return slaviDetectParam;
    }

    private synchronized BufferedImage slaviDetectBufferedImage(BufferedImage bufferedImage) {
        Pointer resultPointer;
        if (false) {
            resultPointer = ImageUtils.convertBufferedImageToRGBPointer(bufferedImage);
        } else {
            resultPointer = ImageUtils.convertBufferedImageToPointer(bufferedImage);
        }

        SlaviDetectParam.ByReference slaviDetectParam = getSlaviDetectParam();
        slaviDetectParam.crop_center_x = bufferedImage.getWidth() / 2;
        slaviDetectParam.crop_center_y = bufferedImage.getHeight() / 2;

        SlaviDetectResult.ByReference slaviDetectResult = new SlaviDetectResult.ByReference();
        //        byte[] imageR = new byte[imageBytes.length];
        Pointer ex8p = new Memory(slaviDetectParam.resize_height * slaviDetectParam.resize_width * Native.getNativeSize(Byte.TYPE));
        try {
            logger.info("进入 slaviDetectBufferedImage detect函数");
            int imageFormat = 3;
            int result = s2iDetect.slaviDetectWithImageProcess(resultPointer, bufferedImage.getWidth(), bufferedImage.getHeight(), imageFormat, slaviDetectParam, slaviDetectResult, ex8p, null);
            logger.info("完成detect函数： " + result + "slaviDetectResult:" + slaviDetectResult.major_version + "." + slaviDetectResult.minor_version + "." + slaviDetectResult.revision_number);
        } catch (Exception e) {

            logger.error(e.getMessage());
        }
        return getSlaviDetectedBufferedImage(ex8p);
    }


    private BufferedImage getSlaviDetectedBufferedImage(Pointer ex8p) {
        BufferedImage bufferedImage = convertPointerToBufferedImage(ex8p, 640, 640);
        return bufferedImage;
    }

    public interface DecodeUtilInterface {
        void decodedSuccess(S2iDecodeParam.ByReference s2iDecodeParam, S2iDecodeScore.ByReference s2iDecodeScore);
    }
}
