package com.paipeng.javafx.webcam.controller;

import com.paipeng.javafx.webcam.utils.CameraUtil;
import com.paipeng.javafx.webcam.utils.CommonUtil;
import com.paipeng.javafx.webcam.utils.DecoderUtil;
import com.paipeng.javafx.webcam.view.NanogridDecoderResultPane;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeParam;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeScore;
import javafx.application.Platform;
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

import java.awt.image.BufferedImage;
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

    @FXML
    private TextField fpsTextField;

    @FXML
    private NanogridDecoderResultPane nanogridDecoderResultPane;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        previewImageView.setImage(new Image(Objects.requireNonNull(WebCamViewController.class.getResourceAsStream("/images/logo.png"))));
        openButton.setOnMouseClicked(event -> {
            logger.trace("openButton clicked");

            if (CameraUtil.getInstance().isWebcamOpened()) {
                stopCamera();
            } else {
                if (CameraUtil.getInstance().hasWebCam()) {
                    startCamera();
                } else {
                    logger.error("no camera found!");
                }
            }
        });

        DecoderUtil.getInstance().initNanogridDecoder();

        CameraUtil.getInstance().setCameraUtilInterface(new CameraUtil.CameraUtilInterface() {
            @Override
            public void webcamOpened() {
                logger.trace("webcamOpened");
                Platform.runLater(() -> openButton.setText(CommonUtil.getString("close_webcam")));
            }

            @Override
            public void webcamClosed() {
                logger.trace("webcamClosed");
                Platform.runLater(() -> openButton.setText(CommonUtil.getString("open_webcam")));
                previewImageView.setImage(new Image(Objects.requireNonNull(WebCamViewController.class.getResourceAsStream("/images/icons/webcam.png"))));
            }

            @Override
            public void updateImage(BufferedImage bufferedImage, double fps) {
                previewImageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

                DecoderUtil.getInstance().doDecodeWithDetect(bufferedImage, decodeUtilInterface);

            }
        });
    }

    private DecoderUtil.DecodeUtilInterface decodeUtilInterface = (s2iDecodeParam, s2iDecodeScore) -> updateView(s2iDecodeParam, s2iDecodeScore);

    public void updateView(S2iDecodeParam.ByReference s2iDecodeParam, S2iDecodeScore.ByReference s2iDecodeScore) {
        logger.trace("updateView");

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

    public void startCamera() {
        CameraUtil.getInstance().start();
    }

    public void stopCamera() {
        CameraUtil.getInstance().stop();
    }
}
