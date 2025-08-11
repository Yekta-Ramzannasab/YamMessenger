package com.yamyam.messenger.client.gui.controller;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.shared.PageNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.regex.Pattern;

public class EmailController {

    private final PageNavigator navigator = new PageNavigator(
            "/com/yamyam/messenger/client/gui/fxml/info.fxml",
            "/com/yamyam/messenger/client/gui/fxml/verify.fxml"
    );

    @FXML
    private TextField emailField;

    @FXML
    private Label errorLabel;

    private final Pattern emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    // Get an instance of the network service
    private final NetworkService networkService = NetworkService.getInstance();

    @FXML
    private void handleNext(ActionEvent event) {
        String email = emailField.getText().trim();

        if (!emailPattern.matcher(email).matches()) {
            errorLabel.setText("Please enter a valid email address.");
            return;
        }

        errorLabel.setText("Sending code to email... please wait.");

        // sample of sending code with 2 second late
        new Thread(() -> {
            try {
                Thread.sleep(2000); // sample connecting to server
            } catch (InterruptedException ignored) {
            }

            // after late go to verify page with code
            javafx.application.Platform.runLater(() -> {
                errorLabel.setText(""); // delete the error
                navigator.goToNext(event);
            });
        }).start();
    }
}
