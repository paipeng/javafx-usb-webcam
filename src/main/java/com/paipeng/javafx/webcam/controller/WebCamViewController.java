package com.paipeng.javafx.webcam.controller;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamLock;
import com.github.sarxos.webcam.WebcamResolution;
import com.github.sarxos.webcam.ds.buildin.natives.Device;
import com.github.sarxos.webcam.ds.buildin.natives.DeviceList;
import com.github.sarxos.webcam.ds.buildin.natives.OpenIMAJGrabber;
import com.paipeng.javafx.webcam.utils.DecoderUtil;
import com.paipeng.javafx.webcam.view.NanogridDecoderResultPane;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeParam;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeScore;
import com.s2icode.jna.utils.ImageUtils;
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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

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

    @FXML
    private NanogridDecoderResultPane nanogridDecoderResultPane;

    public static Webcam selWebCam;
    public static boolean isCapture = false;
    private int webCamIndex;
    private int webCamCounter = 0;

    private VideoTacker videoTacker;


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

        DecoderUtil.getInstance().initNanogridDecoder();
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

    class VideoTacker extends Thread implements DecoderUtil.DecodeUtilInterface {
        @Override
        public void run() {
            while (!isCapture) { // For each 30 millisecond take picture and set it in image view
                try {
                    BufferedImage bufferedImage = selWebCam.getImage();
                    if (bufferedImage != null) {
                        previewImageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
                        //fpsTextField.setText(String.format("FPS: %2f", selWebCam.getFPS()));

                        ImageUtils.saveBufferedImageToBmp(bufferedImage, "/Users/paipeng/Downloads/usb_webcam2.bmp");
                        DecoderUtil.getInstance().doDecodeWithDetect(bufferedImage, this);
                    }
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
            }

            logger.trace("VideoTacker ending ...");
        }

        @Override
        public void decodedSuccess(S2iDecodeParam.ByReference s2iDecodeParam, S2iDecodeScore.ByReference s2iDecodeScore) {
            nanogridDecoderResultPane.updateView(s2iDecodeParam, s2iDecodeScore);
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


}
