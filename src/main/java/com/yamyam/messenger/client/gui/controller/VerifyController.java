package com.yamyam.messenger.client.gui.controller;

import com.yamyam.messenger.shared.PageNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class VerifyController {

    private final PageNavigator navigator = new PageNavigator(
            "/com/yamyam/messenger/client/gui/fxml/email.fxml",
            "/com/yamyam/messenger/client/gui/fxml/create-profile.fxml"
    );

    @FXML
    private TextField codeField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleVerify(ActionEvent event) {
        String code = codeField.getText().trim();

        if (!code.matches("\\d{6}")) {
            errorLabel.setText("Please enter a valid 6-digit code.");
        } else {
            errorLabel.setText("");
            // TODO: checking real code in future
            navigator.goToNext(event);
        }
    }
}
