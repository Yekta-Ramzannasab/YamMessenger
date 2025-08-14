package com.yamyam.messenger.client.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.IOException;
import java.net.URL;

//import java.awt.Taskbar;
//import java.awt.Toolkit;
//import java.awt.Taskbar.Feature;


public class TelegramClientApp extends Application {

    private static Scene scene; // one scene for whole app

    @Override
    public void start(Stage stage) throws IOException {
        // Location of fxml file in resources folder
        URL fxmlLocation = getClass().getResource("/com/yamyam/messenger/client/gui/fxml/main/welcome.fxml");
        Parent root = FXMLLoader.load(fxmlLocation);
        scene = new Scene(root);
        scene.getStylesheets().add(
                getClass().getResource("/com/yamyam/messenger/client/gui/styles/style.css").toExternalForm()
        );



        // Set icon on title bar
        Image appIcon = new Image("/com/yamyam/messenger/client/gui/images/icon.png");
        stage.getIcons().add(appIcon);

        // Optional: Set icon on taskbar (Java 9+ only)
//        if (Taskbar.isTaskbarSupported()) {
//            Taskbar taskbar = Taskbar.getTaskbar();
//            if (taskbar.isSupported(Feature.ICON_IMAGE)) {
//                var awtIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/yamyam/messenger/client/gui/images/icon.png"));
//                taskbar.setIconImage(awtIcon);
//            }
//        }
        stage.setWidth(1000);
        stage.setHeight(700);
        stage.setResizable(false);
        stage.setTitle("Yamyam");
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxmlSimpleName) throws IOException {
        Parent root = FXMLLoader.load(TelegramClientApp.class.getResource(
                "/com/yamyam/messenger/client/gui/fxml/" + fxmlSimpleName + ".fxml"
        ));
        scene.setRoot(root);
    }

    public static void main(String[] args) {
        launch(args);
    }
}