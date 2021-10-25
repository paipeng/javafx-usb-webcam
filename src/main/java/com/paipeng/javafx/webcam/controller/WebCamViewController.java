package com.paipeng.javafx.webcam.controller;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.buildin.natives.Device;
import com.github.sarxos.webcam.ds.buildin.natives.DeviceList;
import com.github.sarxos.webcam.ds.buildin.natives.OpenIMAJGrabber;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class WebCamViewController  implements Initializable {
    public static Logger logger = LoggerFactory.getLogger(WebCamViewController.class);
    private static Stage stage;
    private static final String FXML_FILE = "/fxml/WebCamViewController.fxml";

    @FXML
    private ImageView previewImageView;

    @FXML
    private Button openButton;
    @FXML
    private Button captureButton;

    public static Webcam webcam;
    public static boolean isCapture = false;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        previewImageView.setImage(new Image(Objects.requireNonNull(WebCamViewController.class.getResourceAsStream("/images/logo.png"))));

        searchWebcam();
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
        /* Init camera */
        int webCamIndex = -1;
        if (true) {
            Task<Void> webCamTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    OpenIMAJGrabber g = new OpenIMAJGrabber();
                    logger.info("get list of devices");
                    if (g != null) {
                        DeviceList list = g.getVideoDevices().get();
                        logger.info("list devices");
                        //String[] devides = propertiesUtils.getDeviceName().split(",");
                        for (int i = 0; i < list.getNumDevices(); i++) {
                            Device d = list.getDevice(i).get();
                            logger.info("device: " + i + " id: " + d.getIdentifierStr() + " deviceName:" + d.getNameStr());
                        }
                    }
                    for (Webcam w : Webcam.getWebcams()) {
                        if(!w.getName().contains("FaceTime"))
                            webcam = w;
                    }
                    if(webcam == null)
                        webcam = Webcam.getDefault();
                    Dimension[] sizes = webcam.getViewSizes();
                    webcam.setViewSize(sizes[sizes.length - 1]);
                    webcam.open();

                    //startWebCamStream();
                    new VideoTacker().start(); // Start camera capture
                    return null;
                }
            };

            Thread webCamThread = new Thread(webCamTask);
            webCamThread.setDaemon(true);
            webCamThread.start();


        }

        /*
        try {
            //options = CaptureUtils.getInstance().getCaptureList();
            OpenIMAJGrabber g = new OpenIMAJGrabber();
            logger.info("get list of devices");
            if (g != null) {
                DeviceList list = g.getVideoDevices().get();
                logger.info("list devices");
                //String[] devides = propertiesUtils.getDeviceName().split(",");
                for (int i = 0; i < list.getNumDevices(); i++) {
                    Device d = list.getDevice(i).get();
                    logger.info("device: " + i + " id: " + d.getIdentifierStr() + " deviceName:" + d.getNameStr());
                    webCamIndex = i;
                    //webCamCounter++;
                    //for (int j = 0; j < devides.length; j++) {
                    //    if (d.getNameStr().contains(devides[j])) {
                    //        webCamIndex = i;
                    //    }
                    //}
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        List<Webcam> webcamDeviceList = Webcam.getWebcams();
        for (Webcam webcam1 : webcamDeviceList) {
            logger.trace("Webcam1: " + webcam1.getName());
        }
        webcam = Webcam.getWebcams().get(webCamIndex);
        if(webcam == null) {
            logger.error("Camera not found");
        } else {
            webcam.setViewSize(new Dimension(640, 480));
            webcam.open();
            new VideoTacker().start(); // Start camera capture
        }

         */
    }


    class VideoTacker extends Thread {
        @Override
        public void run() {
            while (!isCapture) { // For each 30 millisecond take picture and set it in image view
                try {
                    previewImageView.setImage(SwingFXUtils.toFXImage(webcam.getImage(), null));
                    sleep(30);
                } catch (InterruptedException ex) {
                    logger.error(ex.getMessage());
                }
            }
        }
    }
}
