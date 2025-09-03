package com.yamyam.messenger.client.gui.controller.chat;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.util.AppSession;
import com.yamyam.messenger.shared.model.Users;
import com.yamyam.messenger.shared.util.PageNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

public class ProfileController {

    private final PageNavigator navigator = new PageNavigator(
            "/com/yamyam/messenger/client/gui/fxml/auth/verify.fxml",
            "/com/yamyam/messenger/client/gui/fxml/chat/chat.fxml"
    );

    private String email ;

    private final NetworkService networkService;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextArea bioField;

    @FXML
    private Label errorLabel;

    public ProfileController() {

        this.networkService = NetworkService.getInstance();
    }

    public void initData( String email) {
        this.email = email;
    }

    @FXML
    private void handleContinue(ActionEvent event) throws IOException {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String bio = bioField.getText().trim();

        if (fullName.isEmpty()) {
            errorLabel.setText("Full Name is required.");
            return;
        }

        errorLabel.setText("");

        Users user ;
        try {
            user = networkService.clientHandleLogin(email);
            AppSession.setCurrentUser(user);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Request to complete user information in the database
        networkService.fillUserFirstProfile(email,fullName,username,bio);

        navigator.goToNext(event);
    }
}