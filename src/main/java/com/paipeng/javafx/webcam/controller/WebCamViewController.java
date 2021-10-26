package com.paipeng.javafx.webcam.controller;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamLock;
import com.github.sarxos.webcam.WebcamResolution;
import com.github.sarxos.webcam.ds.buildin.natives.Device;
import com.github.sarxos.webcam.ds.buildin.natives.DeviceList;
import com.github.sarxos.webcam.ds.buildin.natives.OpenIMAJGrabber;
import com.s2icode.jna.model.S2iCodeImage;
import com.s2icode.jna.nanogrid.decoder.S2iNanogridDecoder;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeConfig;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeInfo;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeParam;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeScore;
import com.s2icode.jna.s2idetect.S2iDetect;
import com.s2icode.jna.s2idetect.model.S2iDetectParam;
import com.s2icode.jna.s2idetect.model.S2iDetectResult;
import com.s2icode.jna.s2idetect.model.SlaviDetectParam;
import com.s2icode.jna.s2idetect.model.SlaviDetectResult;
import com.s2icode.jna.utils.ImageUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static com.s2icode.jna.utils.ImageUtils.convertPointerToBufferedImage;
import static com.s2icode.jna.utils.ImageUtils.readS2iCodeImageFromBufferedImage3;

public class WebCamViewController implements Initializable {
    public static Logger logger = LoggerFactory.getLogger(WebCamViewController.class);
    private static Stage stage;
    private static final String FXML_FILE = "/fxml/WebCamViewController.fxml";

    @FXML
    private ImageView previewImageView;

    @FXML
    private Button openButton;
    @FXML
    private Button captureButton;

    @FXML
    private TextField fpsTextField;

    public static Webcam selWebCam;
    public static boolean isCapture = false;
    private int webCamIndex;
    private int webCamCounter = 0;

    private VideoTacker videoTacker;



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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        previewImageView.setImage(new Image(Objects.requireNonNull(WebCamViewController.class.getResourceAsStream("/images/logo.png"))));
        openButton.setOnMouseClicked(event -> {
            logger.trace("openButton clicked");

            if (isWebcamOpened()) {
                stopWebcam();
            } else {
                if (webCamCounter > 0) {
                    startWebcam();
                } else {
                    logger.error("no camera found!");
                }
            }
        });
        searchWebcam();
        //startWebcam();

        initNanogridDecoder();
    }

    public static void start() {
        logger.trace("start");
        try {
            ResourceBundle resources = ResourceBundle.getBundle("bundles.languages", new Locale("zh", "Zh"));
            Parent root = FXMLLoader.load(Objects.requireNonNull(WebCamViewController.class.getResource(FXML_FILE)), resources);

            Scene scene = new Scene(root);
            stage = new Stage();
            //stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle(resources.getString("title"));
            stage.getIcons().add(new Image(Objects.requireNonNull(WebCamViewController.class.getResourceAsStream("/images/logo.png"))));
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.setResizable(true);
            stage.show();

        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    private void searchWebcam() {
        try {
            //options = CaptureUtils.getInstance().getCaptureList();
            OpenIMAJGrabber g = new OpenIMAJGrabber();
            logger.info("get list of devices");
            DeviceList list = g.getVideoDevices().get();
            logger.info("list devices");
            //String[] devides = propertiesUtils.getDeviceName().split(",");
            for (int i = 0; i < list.getNumDevices(); i++) {
                Device d = list.getDevice(i).get();
                logger.info("device: " + i + " id: " + d.getIdentifierStr() + " deviceName:" + d.getNameStr());
                webCamCounter++;
                if (d.getNameStr().contains("Andonstar Camera")) {
                    webCamIndex = i;
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        logger.trace("webCamIndex: " + webCamIndex);
    }

    private void startWebcam() {
        /* Init camera */
        if (com.sun.jna.Platform.isMac()) {
            Task<Void> webCamTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if (selWebCam == null) {
                        logger.trace("startWebcam: " + webCamIndex);
                        selWebCam = Webcam.getWebcams().get(webCamIndex);
                        logger.info("device name ···" + Webcam.getWebcams().get(webCamIndex).getName());

                        for (Dimension d : selWebCam.getViewSizes()) {
                            logger.info("webcam view size: " + d.toString());
                        }
                        selWebCam.setViewSize(WebcamResolution.VGA.getSize());

                        //selWebCam.setCustomViewSizes(new Dimension[] { WebcamResolution.SXGA.getSize() }); // register custom size
                        //selWebCam.setViewSize(WebcamResolution.SXGA.getSize()); // set size
                        logger.info("webcam view size " + selWebCam.getViewSize().toString());

                        if (selWebCam.open() == true) {
                            logger.info("camera opnened");
                            webcamOpened();
                        } else {
                            // TODO error handling
                            logger.error("camera opnen error");
                        }
                    } else {
                        selWebCam = Webcam.getWebcams().get(webCamIndex);
                        selWebCam.open();
                    }
                    //stopCamera = false;
                    //getPreviewImage();
                    startVideoTacker();
                    return null;
                }
            };
            Thread webCamThread = new Thread(webCamTask);
            webCamThread.setDaemon(true);
            webCamThread.start();
        } else {
            List<Webcam> webcamDeviceList = Webcam.getWebcams();
            if (webcamDeviceList != null) {
                for (Webcam webcam : webcamDeviceList) {
                    logger.info("webcam name " + webcam.getDevice().getName());
                }
            }
            if (selWebCam == null && webcamDeviceList.size() != 0) {

                logger.info("device name：" + Webcam.getWebcams().get(webCamIndex).getName());
                selWebCam = Webcam.getWebcams().get(webCamIndex);
                logger.info("webcam view size " + selWebCam.getViewSize().toString());
                for (Dimension d : selWebCam.getViewSizes()) {
                    logger.info("webcam view size: " + d.getWidth() + "-" + d.getHeight());
                }
                WebcamLock webcamLock = selWebCam.getLock();
                if (webcamLock.isLocked()) {
                    webcamLock.unlock();
                    webcamLock.disable();
                }
                selWebCam.setViewSize(WebcamResolution.VGA.getSize());
                logger.info("webcam view size " + selWebCam.getViewSize().toString());
                try {
                    if (selWebCam.isImageNew() && selWebCam.getDevice() != null) {
                        logger.info("start webcam·····");
                        selWebCam.open();
                        startVideoTacker();
                    } else {
                        logger.error("no ready to start webcam");
                    }
                } catch (WebcamException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    private void stopWebcam() {
        logger.trace("webcamOpened");
        if (isWebcamOpened()) {
            if (videoTacker.isAlive()) {
                isCapture = true;
            }
            videoTacker = null;
            selWebCam.close();
            selWebCam = null;
        }
        webcamClosed();
    }

    private boolean isWebcamOpened() {
        return selWebCam != null && selWebCam.isOpen();
    }

    private void webcamOpened() {
        logger.trace("webcamOpened");
        Platform.runLater(() -> openButton.setText(getString("close_webcam")));

    }

    private void webcamClosed() {
        logger.trace("webcamClosed");
        openButton.setText(getString("open_webcam"));

    }

    private void startVideoTacker() {
        if (videoTacker == null) {
            videoTacker = new VideoTacker();
        }
        if (!videoTacker.isAlive()) {
            isCapture = false;
            videoTacker.start(); // Start camera capture
        }
    }


    class VideoTacker extends Thread {
        @Override
        public void run() {
            while (!isCapture) { // For each 30 millisecond take picture and set it in image view
                try {
                    previewImageView.setImage(SwingFXUtils.toFXImage(selWebCam.getImage(), null));
                    fpsTextField.setText(String.format("FPS: %2f", selWebCam.getFPS()));

                    doDecodeWithDetect(selWebCam.getImage());
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
            }

            logger.trace("VideoTacker ending ...");
        }
    }

    public static String getString(String key) {
        try {
            ResourceBundle resources = ResourceBundle.getBundle("bundles.languages", new Locale("zh", "Zh"));
            return resources.getString(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    private void initNanogridDecoder() {
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

    public void doDecodeWithDetect(BufferedImage bufferedImage) {
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
            logger.trace("decodeParam brand owner name: " + new String(decodeParam.brand_owner_name, 0, 16, "GB18030"));
            logger.trace("decodeParam clientId: " + decodeParam.client_id);
            logger.trace("decodeParam serial_number: " + decodeParam.serial_number);
            logger.trace("decodeParam productId: " + decodeParam.product_id);
            logger.trace("decodeParam data: " + new String(decodeParam.data, 0, decodeParam.data_len, "GB18030"));
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
}
