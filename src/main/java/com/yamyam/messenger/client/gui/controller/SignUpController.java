package com.yamyam.messenger.client.gui.controller;


import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.stage.Stage;
import java.io.IOException;

public class SignUpController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleSignUp(ActionEvent event) {
        String name = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();

        // for now only go to next page , but later for verify email
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/verify.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToLogin(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
