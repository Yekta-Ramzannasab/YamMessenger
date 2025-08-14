package com.yamyam.messenger.client.gui.controller;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.shared.PageNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
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

        // Go to the verification page with the authentication code
        new Thread(() -> {
            Integer verificationCode = networkService.requestVerificationCode(email);

            javafx.application.Platform.runLater(() -> {
                if (verificationCode != null) {
                    try {
                        VerifyController verifyController = navigator.goToNextAndGetController(event);

                        // pass the data to the new controller
                        verifyController.initData(verificationCode,email);

                    } catch (IOException e) {
                        e.printStackTrace();
                        errorLabel.setText("Error: Could not load the next page.");
                    }
                } else {
                    errorLabel.setText("Failed to send code.");
                }
            });
        }).start();
    }
}
