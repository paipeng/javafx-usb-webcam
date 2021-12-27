package com.paipeng.javafx.webcam.utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamLock;
import com.github.sarxos.webcam.WebcamResolution;
import com.github.sarxos.webcam.ds.buildin.natives.Device;
import com.github.sarxos.webcam.ds.buildin.natives.DeviceList;
import com.github.sarxos.webcam.ds.buildin.natives.OpenIMAJGrabber;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class CameraUtil {
    public static Logger logger = LoggerFactory.getLogger(CameraUtil.class);

    public static Webcam selWebCam;
    public static boolean isCapture = false;
    private int webCamIndex;
    private int webCamCounter = 0;

    private VideoTacker videoTacker;
    private CameraUtilInterface cameraUtilInterface;

    private static CameraUtil instance;


    public static CameraUtil getInstance() {
        if (instance == null)
            instance = new CameraUtil();
        instance.searchWebcam();
        return instance;
    }

    private void searchWebcam() {
        try {
            OpenIMAJGrabber g = new OpenIMAJGrabber();
            logger.info("get list of devices");
            DeviceList list = g.getVideoDevices().get();
            logger.info("list devices");
            for (int i = 0; i < list.getNumDevices(); i++) {
                Device d = list.getDevice(i).get();
                logger.info("device: " + i + " id: " + d.getIdentifierStr() + " deviceName:" + d.getNameStr());
                webCamCounter++;
                if (d.getNameStr().contains("Andonstar Camera")) {
                    webCamIndex = i;
                } else if (d.getNameStr().contains("USB")) {
                    webCamIndex = i;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        logger.trace("webCamIndex: " + webCamIndex);
    }

    public void start() {
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
                        logger.info("webcam view size " + selWebCam.getViewSize().toString());

                        if (selWebCam.open() == true) {
                            logger.info("camera opnened");
                            cameraUtilInterface.webcamOpened();
                        } else {
                            // TODO error handling
                            logger.error("camera opnen error");
                        }
                    } else {
                        selWebCam = Webcam.getWebcams().get(webCamIndex);
                        selWebCam.open();
                    }
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


    private void startVideoTacker() {
        if (videoTacker == null) {
            videoTacker = new VideoTacker();
        }
        if (!videoTacker.isAlive()) {
            isCapture = false;
            videoTacker.start(); // Start camera capture
        }
    }

    public void stop() {
        logger.trace("webcamOpened");
        if (isWebcamOpened()) {
            if (videoTacker != null && videoTacker.isAlive()) {
                isCapture = true;
            }
            videoTacker = null;
            selWebCam.close();
            selWebCam = null;
        }
        cameraUtilInterface.webcamClosed();
    }


    public boolean isWebcamOpened() {
        return selWebCam != null && selWebCam.isOpen();
    }

    public CameraUtilInterface getCameraUtilInterface() {
        return cameraUtilInterface;
    }

    public void setCameraUtilInterface(CameraUtilInterface cameraUtilInterface) {
        this.cameraUtilInterface = cameraUtilInterface;
    }

    public boolean hasWebCam() {
        return webCamCounter > 0;
    }

    class VideoTacker extends Thread { //implements DecoderUtil.DecodeUtilInterface {
        @Override
        public void run() {
            while (!isCapture) { // For each 30 millisecond take picture and set it in image view
                try {
                    cameraUtilInterface.updateImage(selWebCam.getImage(), selWebCam.getFPS());
                    /*
                    previewImageView.setImage(SwingFXUtils.toFXImage(selWebCam.getImage(), null));
                    //fpsTextField.setText(String.format("FPS: %2f", selWebCam.getFPS()));

                    DecoderUtil.getInstance().doDecodeWithDetect(selWebCam.getImage(), this);

                     */
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
            }

            logger.trace("VideoTacker ending ...");
        }
//
//        @Override
//        public void decodedSuccess(S2iDecodeParam.ByReference s2iDecodeParam, S2iDecodeScore.ByReference s2iDecodeScore) {
//            nanogridDecoderResultPane.updateView(s2iDecodeParam, s2iDecodeScore);
//        }
    }

    public interface CameraUtilInterface {
        void webcamOpened();
        void webcamClosed();
        void updateImage(BufferedImage bufferedImage, double fps);
    }
}
