package com.paipeng.javafx.webcam.view;

import com.paipeng.javafx.webcam.controller.WebCamViewController;
import com.paipeng.javafx.webcam.utils.AsynchronTaskUtil;
import com.paipeng.javafx.webcam.utils.CommonUtil;
import com.paipeng.javafx.webcam.utils.ZXingUtil;
import com.s2icode.jna.utils.ImageUtils;
import com.s2icode.s2idetect.CodeImage;
import com.s2icode.s2idetect.DotCodeParam;
import com.s2icode.s2idetect.DotCodeResult;
import com.s2icode.s2idetect.S2iDetect;
import com.s2icode.s2idetect.utils.ImageUtil;
import com.sun.jna.Pointer;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
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

    //private BufferedImage dotCodeBufferedImage;
    private String dotCodeData;
    private DotcodeDecoderResultPaneInterface dotcodeDecoderResultPaneInterface;


    private CodeImage.ByReference decodedImage = null;
    private CodeImage.ByReference processedImage = null;
    private static int count = 169;
    private boolean running = false;
    DotCodeResult.ByReference dotCodeResult;

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

    public void setDotcodeDecoderResultPaneInterface(DotcodeDecoderResultPaneInterface dotcodeDecoderResultPaneInterface) {
        this.dotcodeDecoderResultPaneInterface = dotcodeDecoderResultPaneInterface;
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


    public synchronized void decodeDotCode(BufferedImage bufferedImage) {
        logger.trace("decodeDotCode");

        if (bufferedImage != null) {
            Platform.runLater(() -> {
                processedImageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
            });
            CodeImage.ByReference codeImage = ImageUtil.convertBufferedImageToCodeImage(bufferedImage);

            boolean running = AsynchronTaskUtil.startTask(new AsynchronTaskUtil.AsynchronTaskInterface() {
                @Override
                public void doTask() {
                    logger.trace("doTask");
                    doDotCodeDecode(codeImage);
                    logger.trace("doTask end");
                }

                @Override
                public void taskEnd() {
                    logger.trace("taskEnd");
                    updateView();
                }
            });

            if (running) {
                logger.trace("asynchronTask still running, skip this frame");
            }
        }

        //processedImageView.setImage(SwingFXUtils.toFXImage(processedBufferedImage, null));
    }

    private void doDotCodeDecode(CodeImage.ByReference codeImage) {
        logger.trace("doDotCodeDecode");

        //ImageUtils.saveBufferedImageToBmp(bufferedImage, String.format("/Users/paipeng/Downloads/dotcode/preview_%d.bmp", count++));
        String saveFolder = null;//"/Users/paipeng/Downloads/dotcode";

        DotCodeParam.ByReference dotCodeParam = new DotCodeParam.ByReference();

        dotCodeParam.rescale = getRescale();
        dotCodeParam.threshold = getThreshold();
        dotCodeParam.idx = 5;
        dotCodeParam.detect_rotate = 1;
        dotCodeParam.crop_width = 200;
        dotCodeParam.crop_height = 100;
        dotCodeParam.resize_width = 200;
        dotCodeParam.resize_height = 100;

        dotCodeResult = new DotCodeResult.ByReference();

        decodedImage = new CodeImage.ByReference();
        decodedImage.width = (int)(codeImage.width*dotCodeParam.rescale/12);
        decodedImage.height = (int)(codeImage.height*dotCodeParam.rescale/12);

        Pointer resultPointer = com.s2icode.s2idetect.utils.ImageUtil.byteToPointer(new byte[decodedImage.width*decodedImage.height]);
        decodedImage.setDataPointer(resultPointer);


        processedImage = new CodeImage.ByReference();

        processedImage.width = codeImage.width;
        processedImage.height = codeImage.height;
        processedImage.image_format = 0;

        processedImage.setDataPointer(com.s2icode.s2idetect.utils.ImageUtil.byteToPointer(new byte[processedImage.width*processedImage.height]));



        int ret = S2iDetect.dotcodeDecode(codeImage, dotCodeParam, dotCodeResult, decodedImage, processedImage, saveFolder);
        logger.trace("dotcodeDecode ret: " + ret);
        logger.trace("size_idx: " + dotCodeResult.size_idx);
        logger.trace("dotcode_width/dotcode_height: " + dotCodeResult.dotcode_width + "-" + dotCodeResult.dotcode_height);


    }

    public void updateView() {
        logger.trace("updateView");


        //updateView(bufferedImage, dotCodeParam, dotCodeResult, processedBufferedImage);

        BufferedImage cutBufferedImage = com.s2icode.s2idetect.utils.ImageUtil.cropImage(ImageUtil.convertCodeImageToBufferedImaged(decodedImage), 0, 0, dotCodeResult.dotcode_width, dotCodeResult.dotcode_height);
        logger.trace("cutBufferedImage size: " + cutBufferedImage.getWidth() + "-" + cutBufferedImage.getHeight());
        int factor = 4;
        BufferedImage dotCodeBufferedImage = ImageUtils.resizeBufferedImage(cutBufferedImage, cutBufferedImage.getWidth()*factor, cutBufferedImage.getHeight() * factor);
        logger.trace("dotCodeBufferedImage size: " + dotCodeBufferedImage.getWidth() + "-" + dotCodeBufferedImage.getHeight());
        dotCodeData = ZXingUtil.qrCodeDecode(dotCodeBufferedImage);
        logger.trace("dotCodeData: " + dotCodeData);

        BufferedImage processedBufferedImage = ImageUtil.convertCodeImageToBufferedImaged(processedImage);
        dotcodeDecoderResultPaneInterface.updateProcessedBufferedImage(processedBufferedImage);


        Platform.runLater(() -> {
            //processedImageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
            if (dotCodeBufferedImage != null) {
                decodedImageView.setImage(SwingFXUtils.toFXImage(dotCodeBufferedImage, null));
            }
            dataTextField.setText(dotCodeData);
            detectedRotateTextField.setText(String.format("%2.2f (filterSize: %d)", dotCodeResult.detected_rotate, dotCodeResult.size_idx));

        });
    }

    public interface DotcodeDecoderResultPaneInterface {
        void updateProcessedBufferedImage(BufferedImage processedBufferedImage);
    }
}
