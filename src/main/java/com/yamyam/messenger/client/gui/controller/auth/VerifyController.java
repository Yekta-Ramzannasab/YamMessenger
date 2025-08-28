package com.yamyam.messenger.client.gui.controller.auth;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.shared.util.PageNavigator;
import com.yamyam.messenger.shared.model.Users;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import com.yamyam.messenger.client.util.AppSession;
import com.yamyam.messenger.shared.model.UserProfile;

public class VerifyController {

    private final PageNavigator navigator = new PageNavigator(
            "/com/yamyam/messenger/client/gui/fxml/auth/email.fxml",
            "/com/yamyam/messenger/client/gui/fxml/chat/create-profile.fxml"
    );

    @FXML
    private TextField codeField;

    @FXML
    private Label errorLabel;

    private Integer correctCode;

    private String email ;

    private final NetworkService networkService;

    public VerifyController() {

        this.networkService = NetworkService.getInstance();
    }

    // This method is called to provide the correct code to this controller
    public void initData(Integer code , String email) {
        this.correctCode = code;
        this.email = email;
    }

    @FXML
    private void handleVerify(ActionEvent event) {
        String userInput = codeField.getText().trim();

        try {
            int userCode = Integer.parseInt(userInput);

            // compare the code entered by the user with the correct code
            if (correctCode.equals(userCode)) {
                errorLabel.setText("Verification Successful!");

                Users user ;
                try {
                    user = networkService.clientHandleLogin(email);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                if(user.isVerified()){
                    navigator.goToNext(event);
                } else {
                    navigator.goToNext(event);
                }
            } else {
                errorLabel.setText("Incorrect code. Please try again.");
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Please enter a valid number.");
        }
    }
}
