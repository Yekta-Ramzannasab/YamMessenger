module com.yam.messenger {
    // requirement for core JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;

    // requirement of GSON and SQL library to work with JSON and PostgresSQL
    requires com.google.gson;
    requires java.sql;

    // allowing the fxml module to access your controllers deeply
    opens com.yamyam.messenger.client.gui to javafx.fxml;

    // making the main program package and shared classes available for execution
    exports com.yamyam.messenger.client.gui;
    exports com.yamyam.messenger.shared;
}