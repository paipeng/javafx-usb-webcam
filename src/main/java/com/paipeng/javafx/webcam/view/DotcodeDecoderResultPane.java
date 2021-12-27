package com.paipeng.javafx.webcam.view;

import com.s2icode.jna.utils.ImageUtils;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class DotcodeDecoderResultPane  extends Pane {
    public static Logger logger = LoggerFactory.getLogger(DotcodeDecoderResultPane.class);

    private static final String PREFIX = File.separator + "fxml" + File.separator;

    @FXML
    private ImageView decodedImageView;

    @FXML
    private TextField dataTextField;

    @FXML
    private Slider rescaleSlider;

    @FXML
    private Slider thresholdSlider;

    public DotcodeDecoderResultPane() {
        super();
        ResourceBundle resources = ResourceBundle.getBundle("bundles.languages", new Locale("zh", "Zh"));
        //Parent root = FXMLLoader.load(MainViewController.class.getResource(FXML_FILE), resources);

        FXMLLoader loader = new FXMLLoader();
        loader.setRoot(this);
        loader.setControllerFactory(theClass -> this);

        String fileName = PREFIX + this.getClass().getSimpleName() + ".fxml";
        try {
            loader.setResources(resources);
            loader.load(this.getClass().getResourceAsStream(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        initView();
    }

    private void initView() {
        logger.trace("initView");
        dataTextField.setText("");
        rescaleSlider.setValue(1);
        thresholdSlider.setValue(100);
    }

    public float getRescale() {
        return (float)rescaleSlider.getValue();
    }


    public int getThreshold() {
        return (int)thresholdSlider.getValue();
    }

    public void updateView(BufferedImage bufferedImage) {
        if (bufferedImage != null) {
            decodedImageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
            ImageUtils.saveBufferedImageToBmp(bufferedImage, "/Users/paipeng/Downloads/dotcode/decodedimage.bmp");
        }
    }
}
