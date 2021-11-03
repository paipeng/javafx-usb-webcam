package com.paipeng.javafx.webcam.utils;

import java.util.Locale;
import java.util.ResourceBundle;

public class CommonUtil {

    public static String getString(String key) {
        try {
            ResourceBundle resources = ResourceBundle.getBundle("bundles.languages", new Locale("zh", "Zh"));
            return resources.getString(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
