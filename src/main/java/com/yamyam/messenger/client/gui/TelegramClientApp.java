package com.yamyam.messenger.client.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.IOException;
import java.net.URL;

import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.Taskbar.Feature;


public class TelegramClientApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Location of fxml file in resources folder
        URL fxmlLocation = getClass().getResource("/fxml/welcome.fxml");
        Parent root = FXMLLoader.load(fxmlLocation);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        // Set icon on title bar
        Image appIcon = new Image("/images/icon.png");
        stage.getIcons().add(appIcon);

        // Optional: Set icon on taskbar (Java 9+ only)
        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Feature.ICON_IMAGE)) {
                var awtIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/icon.png"));
                taskbar.setIconImage(awtIcon);
            }
        }
        stage.setWidth(1000);
        stage.setHeight(700);
        stage.setResizable(false);
        stage.setTitle("Yamyam");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}