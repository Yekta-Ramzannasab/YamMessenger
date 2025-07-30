package com.yamyam.messenger.client.gui.controller;

import com.yamyam.messenger.shared.PageNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class InfoController {
    private final PageNavigator navigator = new PageNavigator(
            "/com/yamyam/messenger/client/gui/fxml/welcome.fxml",
            "/com/yamyam/messenger/client/gui/fxml/email.fxml"
    );

    @FXML
    private void goToPreviousPage(ActionEvent event) {
        navigator.goToPrevious(event);
    }

    @FXML
    private void goToNextPage(ActionEvent event) {
        navigator.goToNext(event);
    }
}
