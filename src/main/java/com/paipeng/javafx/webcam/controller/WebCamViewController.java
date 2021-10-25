package com.paipeng.javafx.webcam.controller;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URL;
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

    public static Webcam selWebCam;
    public static boolean isCapture = false;
    private int webCamIndex;
    private int webCamCounter = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        previewImageView.setImage(new Image(Objects.requireNonNull(WebCamViewController.class.getResourceAsStream("/images/logo.png"))));
        openButton.setOnMouseClicked(event -> {
            logger.trace("openButton clicked");
            if (webCamCounter > 0) {
                startWebcam();
            } else {
                logger.error("no camera found!");
            }
        });
        searchWebcam();
        //startWebcam();
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
        if (true) {
            Task<Void> webCamTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if (selWebCam == null) {
                        logger.trace("startWebcam: " + webCamIndex);
                        selWebCam = Webcam.getWebcams().get(webCamIndex);
                        logger.info("device name ···" + Webcam.getWebcams().get(webCamIndex).getName());
                        //logger.info("webcam view size " + selWebCam.getViewSize().toString());

                        for (Dimension d : selWebCam.getViewSizes()) {
                            logger.info("webcam view size: " + d.toString());
                        }
                        //setCaptureDimension(selWebCam);
                        selWebCam.setViewSize(WebcamResolution.VGA.getSize());

                        //selWebCam.setCustomViewSizes(new Dimension[] { WebcamResolution.SXGA.getSize() }); // register custom size
                        //selWebCam.setViewSize(WebcamResolution.SXGA.getSize()); // set size
                        logger.info("webcam view size " + selWebCam.getViewSize().toString());

                        if (selWebCam.open() == true) {
                            logger.info("camera opnened");
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

                    new VideoTacker().start(); // Start camera capture
                    return null;
                }
            };

            Thread webCamThread = new Thread(webCamTask);
            webCamThread.setDaemon(true);
            webCamThread.start();


        }
    }


    class VideoTacker extends Thread {
        @Override
        public void run() {
            while (!isCapture) { // For each 30 millisecond take picture and set it in image view
                try {
                    previewImageView.setImage(SwingFXUtils.toFXImage(selWebCam.getImage(), null));
                    sleep(30);
                } catch (InterruptedException ex) {
                    logger.error(ex.getMessage());
                }
            }
        }
    }
}
