package com.paipeng.javafx.webcam.view;

import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeParam;
import com.s2icode.jna.nanogrid.decoder.model.S2iDecodeScore;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

public class NanogridDecoderResultPane extends Pane {
    public static Logger logger = LoggerFactory.getLogger(NanogridDecoderResultPane.class);

    private static final String PREFIX = File.separator + "fxml" + File.separator;

    @FXML
    private TextField brandOwnernameTextField;

    @FXML
    private TextField serailNumberTextField;

    @FXML
    private TextField timestampTextField;

    @FXML
    private TextArea nanogridDataTextArea;

    @FXML
    private TextField decodeScoreTextField;

    public NanogridDecoderResultPane() {
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
    }

    private void initView() {
        logger.trace("initView");
        brandOwnernameTextField.setText("");
        serailNumberTextField.setText("");
        timestampTextField.setText("");

        nanogridDataTextArea.setText("");

        decodeScoreTextField.setText("");
    }

    public void updateView(S2iDecodeParam.ByReference s2iDecodeParam, S2iDecodeScore.ByReference s2iDecodeScore) {
        logger.trace("updateView");
        Platform.runLater(() -> {
            try {
                if (s2iDecodeParam != null && s2iDecodeScore != null) {
                    brandOwnernameTextField.setText(new String(s2iDecodeParam.brand_owner_name, 0, 30, "GB18030"));
                    serailNumberTextField.setText(String.format("%04d%08d%04d", s2iDecodeParam.client_id, s2iDecodeParam.serial_number.intValue(), s2iDecodeParam.product_id));

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    Date date = new Date(s2iDecodeParam.timestamp.longValue() * 1000);
                    timestampTextField.setText(simpleDateFormat.format(date));

                    if (s2iDecodeParam.data_len <= 128) {
                        nanogridDataTextArea.setText(new String(s2iDecodeParam.data, 0, s2iDecodeParam.data_len, "GB18030"));
                    } else {
                        logger.error("data len invalid");
                    }
                    decodeScoreTextField.setText(String.format(" score: %2.02f (nano: %3d)", s2iDecodeScore.imageQuality, s2iDecodeScore.nanoGridCoefficient));
                } else {
                    initView();
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        });

    }
}
