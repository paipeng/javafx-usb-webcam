package com.paipeng.javafx.webcam.controller;

import com.paipeng.javafx.webcam.utils.CameraUtil;
import com.paipeng.javafx.webcam.utils.CommonUtil;
import com.paipeng.javafx.webcam.utils.DecoderUtil;
import com.paipeng.javafx.webcam.view.DotcodeDecoderResultPane;
import com.paipeng.javafx.webcam.view.NanogridDecoderResultPane;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeParam;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeScore;
import com.s2icode.jna.utils.ImageUtils;
import com.s2icode.s2idetect.CodeImage;
import com.s2icode.s2idetect.DotCodeParam;
import com.s2icode.s2idetect.S2iDetect;
import com.s2icode.s2idetect.utils.ImageUtil;
import com.sun.jna.Pointer;
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

    //@FXML
    //private NanogridDecoderResultPane nanogridDecoderResultPane;

    @FXML
    private DotcodeDecoderResultPane dotcodeDecoderResultPane;


    private CodeImage.ByReference decodedImage = null;
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

        //DecoderUtil.getInstance().initNanogridDecoder();

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
                previewImageView.setImage(new Image(Objects.requireNonNull(WebCamViewController.class.getResourceAsStream("/images/logo.png"))));
            }

            @Override
            public synchronized void updateImage(BufferedImage bufferedImage, double fps) {
                if (bufferedImage != null) {
                    previewImageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
                    ImageUtils.saveBufferedImageToBmp(bufferedImage, "/Users/paipeng/Downloads/dotcode/preview.bmp");
                    String saveFolder = null;//"/Users/paipeng/Downloads/dotcode";

                    CodeImage.ByReference codeImage = ImageUtil.convertBufferedImageToCodeImage(bufferedImage);
                    DotCodeParam.ByReference dotCodeParam = new DotCodeParam.ByReference();

                    dotCodeParam.rescale = dotcodeDecoderResultPane.getRescale();
                    dotCodeParam.threshold = dotcodeDecoderResultPane.getThreshold();

                    decodedImage = new CodeImage.ByReference();
                    decodedImage.width = (int)(codeImage.width*dotCodeParam.rescale/12);
                    decodedImage.height = (int)(codeImage.height*dotCodeParam.rescale/12);

                    Pointer resultPointer = com.s2icode.s2idetect.utils.ImageUtil.byteToPointer(new byte[decodedImage.width*decodedImage.height]);
                    decodedImage.setDataPointer(resultPointer);


                    int ret = S2iDetect.dotcodeDecode(codeImage, dotCodeParam, decodedImage, saveFolder);
                    logger.trace("dotcodeDecode ret: " + ret);
                    logger.trace("size_idx: " + dotCodeParam.size_idx);
                    logger.trace("dotcode_width/dotcode_height: " + dotCodeParam.dotcode_width + "-" + dotCodeParam.dotcode_height);
                    Platform.runLater(() -> dotcodeDecoderResultPane.updateView(ImageUtil.convertCodeImageToBufferedImaged(decodedImage), dotCodeParam));


                    //DecoderUtil.getInstance().doDecodeWithDetect(bufferedImage, decodeUtilInterface);
                }
            }
        });
    }

    private DecoderUtil.DecodeUtilInterface decodeUtilInterface = (s2iDecodeParam, s2iDecodeScore) -> updateView(s2iDecodeParam, s2iDecodeScore);

    public void updateView(S2iDecodeParam.ByReference s2iDecodeParam, S2iDecodeScore.ByReference s2iDecodeScore) {
        logger.trace("updateView");

        //nanogridDecoderResultPane.updateView(s2iDecodeParam, s2iDecodeScore);
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
