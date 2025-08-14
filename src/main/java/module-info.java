module com.yam.messenger {
    // requirement for core JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;

    // requirement of GSON and SQL library to work with JSON and PostgresSQL
    requires com.google.gson;
    requires java.sql;

    // requirement of Email service
    requires org.simplejavamail;
    requires org.slf4j;
    requires jakarta.mail;
    requires org.simplejavamail.core;

    // allowing the fxml module to access your controllers deeply
    opens com.yamyam.messenger.client.gui to javafx.fxml;
    opens com.yamyam.messenger.client.gui.controller to javafx.fxml;

    // making the main program package and shared classes available for execution
    exports com.yamyam.messenger.client.gui;
    exports com.yamyam.messenger.shared;
    opens com.yamyam.messenger.client.gui.controller.main to javafx.fxml;
    opens com.yamyam.messenger.client.gui.controller.auth to javafx.fxml;
    opens com.yamyam.messenger.client.gui.controller.chat to javafx.fxml;
    exports com.yamyam.messenger.server.database;
    exports com.yamyam.messenger.shared.util;
    exports com.yamyam.messenger.shared.model;
}