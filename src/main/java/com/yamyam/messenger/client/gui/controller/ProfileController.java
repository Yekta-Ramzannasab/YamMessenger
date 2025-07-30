package com.yamyam.messenger.client.gui.controller;

import com.yamyam.messenger.shared.PageNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ProfileController {

    private final PageNavigator navigator = new PageNavigator(
            "/com/yamyam/messenger/client/gui/fxml/verify.fxml",
            "/com/yamyam/messenger/client/gui/fxml/main-view.fxml"
    );

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextArea bioField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleContinue(ActionEvent event) {
        String fullName = fullNameField.getText().trim();

        if (fullName.isEmpty()) {
            errorLabel.setText("Full Name is required.");
            return;
        }

        errorLabel.setText("");

        // sample database to use
        System.out.println("Full Name: " + fullName);
        System.out.println("Username: " + usernameField.getText().trim());
        System.out.println("Bio: " + bioField.getText().trim());

        navigator.goToNext(event);
    }
}
