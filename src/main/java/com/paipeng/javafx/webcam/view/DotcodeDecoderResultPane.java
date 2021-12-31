package com.paipeng.javafx.webcam.view;

import com.paipeng.javafx.webcam.utils.ZXingUtil;
import com.s2icode.jna.utils.ImageUtils;
import com.s2icode.s2idetect.DotCodeParam;
import com.s2icode.s2idetect.DotCodeResult;
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
    private ImageView processedImageView;
    @FXML
    private TextField dataTextField;

    @FXML
    private Slider rescaleSlider;

    @FXML
    private Slider thresholdSlider;

    @FXML
    private TextField detectedRotateTextField;

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
        rescaleSlider.setValue(0.555);
        rescaleSlider.setValue(1.0);
        thresholdSlider.setValue(120);
        rescaleSlider.setShowTickLabels(true);
        rescaleSlider.setShowTickMarks(true);

        thresholdSlider.setShowTickLabels(true);
        thresholdSlider.setShowTickMarks(true);
    }

    public float getRescale() {
        return (float)rescaleSlider.getValue();
    }


    public int getThreshold() {
        return (int)thresholdSlider.getValue();
    }

    public void updateView(BufferedImage bufferedImage, DotCodeParam.ByReference dotCodeParam, DotCodeResult.ByReference dotCodeResult, BufferedImage processedBufferedImage) {
        if (bufferedImage != null) {
            processedImageView.setImage(SwingFXUtils.toFXImage(processedBufferedImage, null));


            BufferedImage cutBufferedImage = com.s2icode.s2idetect.utils.ImageUtil.cropImage(bufferedImage, 0, 0, dotCodeResult.dotcode_width, dotCodeResult.dotcode_height);
            logger.trace("cutBufferedImage size: " + cutBufferedImage.getWidth() + "-" + cutBufferedImage.getHeight());
            int factor = 4;
            BufferedImage resizeBufferedImage = ImageUtils.resizeBufferedImage(cutBufferedImage, cutBufferedImage.getWidth()*factor, cutBufferedImage.getHeight() * factor);
            logger.trace("resizeBufferedImage size: " + resizeBufferedImage.getWidth() + "-" + resizeBufferedImage.getHeight());
            decodedImageView.setImage(SwingFXUtils.toFXImage(resizeBufferedImage, null));
            String data = ZXingUtil.qrCodeDecode(resizeBufferedImage);
            dataTextField.setText(data);

            detectedRotateTextField.setText(String.format("%2.2f (filterSize: %d)", dotCodeResult.detected_rotate, dotCodeResult.size_idx));
            //ImageUtils.saveBufferedImageToBmp(resizeBufferedImage, "/Users/paipeng/Downloads/dotcode/decodedimage.bmp");
        }
    }
}
