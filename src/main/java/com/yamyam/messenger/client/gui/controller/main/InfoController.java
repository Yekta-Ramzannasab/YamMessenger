package com.yamyam.messenger.client.gui.controller.main;

import com.yamyam.messenger.shared.util.PageNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class InfoController {
    private final PageNavigator navigator = new PageNavigator(
            "/com/yamyam/messenger/client/gui/fxml/main/welcome.fxml",
            "/com/yamyam/messenger/client/gui/fxml/auth/email.fxml"
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
