package com.yamyam.messenger.shared;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class PageNavigator {
    private final String previousPage;
    private final String nextPage;

    public PageNavigator(String previousPage, String nextPage) {
        this.previousPage = previousPage;
        this.nextPage = nextPage;
    }
    public void goToPrevious(ActionEvent event) {
        loadPage(event, previousPage);
    }

    public void goToNext(ActionEvent event) {
        loadPage(event, nextPage);
    }

    private void loadPage(ActionEvent event, String pagePath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(pagePath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            //stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
