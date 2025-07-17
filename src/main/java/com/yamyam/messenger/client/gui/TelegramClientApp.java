package com.yamyam.messenger.client.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class TelegramClientApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Location of fxml file in resources folder
        URL fxmlLocation = getClass().getResource("/com/yamyam/messenger/client/gui/fxml/main-view.fxml");
        Parent root = FXMLLoader.load(fxmlLocation);
        Scene scene = new Scene(root, 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}