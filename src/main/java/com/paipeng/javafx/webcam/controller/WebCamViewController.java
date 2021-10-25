package com.paipeng.javafx.webcam.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        previewImageView.setImage(new Image(Objects.requireNonNull(WebCamViewController.class.getResourceAsStream("/images/logo.png"))));

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
}
