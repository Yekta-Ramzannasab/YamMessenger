package com.yamyam.messenger.client.gui;

import com.yamyam.messenger.client.gui.theme.ThemeManager;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class TelegramClientApp extends Application {

    private static Scene scene;                                // Global scene for the app
    private static StackPane appRoot;                          // Root container used for switching views
    private static final Map<String, Parent> cache = new HashMap<>(); // Cache for loaded FXML views

    @Override
    public void start(Stage stage) throws IOException {
        // 1) Create the root and main scene
        appRoot = new StackPane();
        scene = new Scene(appRoot, 1000, 700);

        // 2) Add global stylesheets (scoped safely with .themed.chat-root)
        addStylesheetIfMissing(scene, "/com/yamyam/messenger/client/gui/styles/style.css");
        addStylesheetIfMissing(scene, "/com/yamyam/messenger/client/gui/styles/chat.css");

        // 3) Remove initial white flash background
        scene.setFill(Color.web("#0B0F1A"));

        // 4) Apply saved or default theme
        ThemeManager.apply(scene, ThemeManager.current());

        // 5) Configure stage (icon, title, etc.)
        stage.getIcons().add(new Image("/com/yamyam/messenger/client/gui/images/icon.png"));
        stage.setTitle("Yamyam");
        stage.setResizable(false);
        stage.setScene(scene);

        // 6) Show welcome screen as the first view (no animation)
        Parent welcome = loadView("main/welcome");
        ensureThemeClasses("main/welcome", welcome);
        showInstant(welcome);

        // 7) Preload chat view in the same scene (CSS/layout resolved but stays hidden)
        preloadForNoFlash("chat/chat");

        stage.show();
    }

    /** Public API used by controllers to switch the root view */
    public static void setRoot(String fxmlSimpleName) throws IOException {
        Parent view = cache.computeIfAbsent(fxmlSimpleName, TelegramClientApp::loadOrThrow);
        ensureThemeClasses(fxmlSimpleName, view);
        // Because the view is already preloaded in this Scene, no white flash will appear
        showWithFade(view);
    }

    /* ................. Navigation / Loading .................. */

    private static Parent loadOrThrow(String fxmlSimpleName) {
        try { return loadView(fxmlSimpleName); }
        catch (RuntimeException e) { throw e; }
    }

    private static Parent loadView(String fxmlSimpleName) {
        return cache.computeIfAbsent(fxmlSimpleName, key -> {
            try {
                URL url = TelegramClientApp.class.getResource(
                        "/com/yamyam/messenger/client/gui/fxml/" + key + ".fxml");
                FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(url, key + ".fxml not found"));
                return loader.load();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    /** Preload view into the same Scene to prevent white flash when navigating */
    private static void preloadForNoFlash(String fxmlSimpleName) {
        Parent view = loadView(fxmlSimpleName);
        ensureThemeClasses(fxmlSimpleName, view);

        // Temporarily add to scene graph but keep it hidden and unmanaged
        if (!appRoot.getChildren().contains(view)) {
            view.setVisible(false);
            view.setManaged(false);
            appRoot.getChildren().add(view);
        }

        // Force CSS and layout resolution now (since it's in the same Scene)
        ThemeManager.reapply(scene);
        view.applyCss();
        view.layout();

        try { view.snapshot(new javafx.scene.SnapshotParameters(), null); } catch (Throwable ignore) {}


        // Keep it hidden until explicitly shown later
        view.setVisible(false);
        view.setManaged(false);
    }

    private static void ensureThemeClasses(String fxmlSimpleName, Parent root) {
        var classes = root.getStyleClass();
        if (!classes.contains("themed")) classes.add("themed");
        // Apply additional style class only for chat views
        if (fxmlSimpleName.toLowerCase().contains("chat") && !classes.contains("chat-root")) {
            classes.add("chat-root");
        }
    }

    private static void addStylesheetIfMissing(Scene s, String resourcePath) {
        URL url = TelegramClientApp.class.getResource(resourcePath);
        if (url != null) {
            String css = url.toExternalForm();
            if (!s.getStylesheets().contains(css)) s.getStylesheets().add(css);
        }
    }

    /* ................. View Transitions ..................*/

    private static void showInstant(Parent view) {
        ThemeManager.reapply(scene);
        // Clear everything and show only this view
        appRoot.getChildren().setAll(view);
        view.setVisible(true);
        view.setManaged(true);
        view.applyCss();
        view.layout();
    }

    private static void showWithFade(Parent view) {
        ThemeManager.reapply(scene);

        // Add to root if not already present
        if (!appRoot.getChildren().contains(view)) appRoot.getChildren().add(view);

        // Hide all other children except this view
        for (var n : appRoot.getChildren()) {
            if (n != view) { n.setVisible(false); n.setManaged(false); }
        }
        view.setManaged(true);
        view.setOpacity(0);
        view.setVisible(true);
        view.applyCss();
        view.layout();

        FadeTransition ft = new FadeTransition(Duration.millis(140), view);
        ft.setToValue(1.0);
        ft.play();
    }

    public static void main(String[] args) { launch(args); }
}
