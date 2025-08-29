module com.yam.messenger {
    // requirement for core JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;

    // requirement of GSON and SQL library to work with JSON and PostgresSQL
    requires com.google.gson;

    // requirement of Email service
    requires org.simplejavamail;
    requires jakarta.mail;
    requires org.simplejavamail.core;
    requires com.zaxxer.hikari;
    requires java.sql;
    requires jdk.httpserver;
    requires java.desktop;
    requires okhttp3;

    // allowing the fxml module to access your controllers deeply
    opens com.yamyam.messenger.client.gui to javafx.fxml;
    opens com.yamyam.messenger.client.gui.controller to javafx.fxml;



    // making the main program package and shared classes available for execution

    opens com.yamyam.messenger.client.gui.controller.main to javafx.fxml;
    opens com.yamyam.messenger.client.gui.controller.auth to javafx.fxml;
    opens com.yamyam.messenger.client.gui.controller.chat to javafx.fxml;
    opens com.yamyam.messenger.client.gui.theme to javafx.fxml;

    exports com.yamyam.messenger.client.gui;
    exports com.yamyam.messenger.server.database;
    exports com.yamyam.messenger.shared.util;
    exports com.yamyam.messenger.shared.model;
    exports com.yamyam.messenger.client.gui.theme;
}