package com.yamyam.messenger.client.gui.controller.auth;

public class SignUpController {
//    @FXML private TextField usernameField;
//    @FXML private TextField emailField;
//    @FXML private CheckBox termsCheckbox;
//    @FXML private Label errorLabel;
//
//    @FXML
//    private void handleSignUp(ActionEvent event) {
//        String username = usernameField.getText().trim();
//        String email = emailField.getText().trim();
//
//
//        if (username.isEmpty() || email.isEmpty()) {
//            showError("Please fill in all fields.");
//            return;
//        }
//        if (!username.matches("^[a-zA-Z0-9_]{3,16}$")) {
//            showError("Invalid username. Use 3–16 letters, numbers, or underscores.");
//            return;
//        }
//        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$")) {
//            showError("Invalid email address.");
//            return;
//        }
//        if (!termsCheckbox.isSelected()) {
//            //showError("You must accept the terms and conditions.");
//            errorLabel.setText("Please accept the terms and conditions.");
//            return;
//        }
//        errorLabel.setText("");
//        goToVerifyPage(event);
//    }
//
//    private void showError(String message) {
//        errorLabel.setText(message);
//    }
//
//    private void goToVerifyPage(ActionEvent event) {
//
//        // for now only go to next page , but later for verify email
//
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/yamyam/messenger/client/gui/fxml/verify.fxml"));
//            Parent root = loader.load();
//            Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
//            stage.setScene(new Scene(root));
//            stage.show();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    @FXML
//    private void handleGoToLogin(MouseEvent event) {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/yamyam/messenger/client/gui/fxml/login.fxml"));
//            Parent root = loader.load();
//            Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
//            stage.setScene(new Scene(root));
//            stage.show();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @FXML
//    private void handleBackToWelcome(MouseEvent event) {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/yamyam/messenger/client/gui/fxml/welcome.fxml"));
//            Parent root = loader.load();
//            Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
//            stage.setScene(new Scene(root));
//            stage.show();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}



//package com.yamyam.messenger.client.gui.controller;
//
//
//import javafx.fxml.FXML;
//import javafx.scene.control.*;
//import javafx.scene.input.MouseEvent;
//import javafx.event.ActionEvent;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.*;
//import javafx.stage.Stage;
//import java.io.IOException;
//
//public class SignUpController {
//    @FXML private TextField nameField;
//    @FXML private TextField emailField;
//    @FXML private PasswordField passwordField;
//
//    @FXML
//    private void handleSignUp(ActionEvent event) {
//        String name = nameField.getText();
//        String email = emailField.getText();
//        String password = passwordField.getText();
//
//        // for now only go to next page , but later for verifiy email
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/verify.fxml"));
//            Parent root = loader.load();
//            Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
//            stage.setScene(new Scene(root));
//            stage.show();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @FXML
//    private void handleGoToLogin(MouseEvent event) {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
//            Parent root = loader.load();
//            Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
//            stage.setScene(new Scene(root));
//            stage.show();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
