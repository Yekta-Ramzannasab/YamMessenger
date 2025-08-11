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

    private Integer correctCode;

    // This method is called to provide the correct code to this controller
    public void initData(Integer code) {
        this.correctCode = code;
    }

    @FXML
    private void handleVerify(ActionEvent event) {
        String userInput = codeField.getText().trim();

        try {
            int userCode = Integer.parseInt(userInput);

            // compare the code entered by the user with the correct code
            if (correctCode.equals(userCode)) {
                errorLabel.setText("Verification Successful!");
                navigator.goToNext(event);
            } else {
                errorLabel.setText("Incorrect code. Please try again.");
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Please enter a valid number.");
        }
    }
}
