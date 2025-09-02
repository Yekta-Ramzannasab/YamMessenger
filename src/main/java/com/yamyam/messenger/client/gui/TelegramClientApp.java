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

// ===== wiring (real backend) =====
import com.yamyam.messenger.client.util.ServiceLocator;

import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.impl.NetworkContactServiceAdapter;
import com.yamyam.messenger.client.network.impl.NetworkChatServiceAdapter;
import com.yamyam.messenger.client.network.impl.UsersServiceAdapter;




public class TelegramClientApp extends Application {

    private static Scene scene;                  // Global scene
    private static StackPane appRoot;            // Single root for view switching
    private static final Map<String, Parent> cache = new HashMap<>();

    @Override
    public void start(Stage stage) throws IOException {
        // 1) root + scene
        appRoot = new StackPane();
        scene = new Scene(appRoot, 1000, 700);

        // 2) global css
        addStylesheetIfMissing(scene, "/com/yamyam/messenger/client/gui/styles/style.css");
        addStylesheetIfMissing(scene, "/com/yamyam/messenger/client/gui/styles/chat.css");

        // 3) remove initial white flash
        scene.setFill(Color.web("#0B0F1A"));

        // 4) theme
        ThemeManager.apply(scene, ThemeManager.current());

        // 4.5) WIRE services (DEV → mocks)
        var net = NetworkService.getInstance();
        ServiceLocator.set(new NetworkContactServiceAdapter(net));
        ServiceLocator.set(new NetworkChatServiceAdapter(net));
        ServiceLocator.set(new UsersServiceAdapter(net));

        // 5) stage
        stage.getIcons().add(new Image("/com/yamyam/messenger/client/gui/images/icon.png"));
        stage.setTitle("Yamyam");
        stage.setResizable(false);
        stage.setScene(scene);

        // 6) first view
        Parent welcome = loadView("main/welcome");
        ensureThemeClasses("main/welcome", welcome);
        showInstant(welcome);

        // 7) preload chat to avoid flash
        preloadForNoFlash("chat/chat");

        stage.show();
    }

    public static void setRoot(String fxmlSimpleName) throws IOException {
        Parent view = cache.computeIfAbsent(fxmlSimpleName, TelegramClientApp::loadOrThrow);
        ensureThemeClasses(fxmlSimpleName, view);
        showWithFade(view);
    }

    /* ---------------- loading ---------------- */

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

    private static void preloadForNoFlash(String fxmlSimpleName) {
        Parent view = loadView(fxmlSimpleName);
        ensureThemeClasses(fxmlSimpleName, view);

        if (!appRoot.getChildren().contains(view)) {
            view.setVisible(false);
            view.setManaged(false);
            appRoot.getChildren().add(view);
        }

        ThemeManager.reapply(scene);
        view.applyCss();
        view.layout();
        try { view.snapshot(new javafx.scene.SnapshotParameters(), null); } catch (Throwable ignore) {}

        view.setVisible(false);
        view.setManaged(false);
    }

    private static void ensureThemeClasses(String fxmlSimpleName, Parent root) {
        var classes = root.getStyleClass();
        if (!classes.contains("themed")) classes.add("themed");
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

    /* ---------------- transitions ---------------- */

    private static void showInstant(Parent view) {
        ThemeManager.reapply(scene);
        appRoot.getChildren().setAll(view);
        view.setVisible(true);
        view.setManaged(true);
        view.applyCss();
        view.layout();
    }

    private static void showWithFade(Parent view) {
        ThemeManager.reapply(scene);
        if (!appRoot.getChildren().contains(view)) appRoot.getChildren().add(view);

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



//package com.yamyam.messenger.client.gui;
//
//import com.yamyam.messenger.client.gui.theme.ThemeManager;
//import javafx.animation.FadeTransition;
//import javafx.application.Application;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.scene.image.Image;
//import javafx.scene.layout.StackPane;
//import javafx.scene.paint.Color;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//
//import java.io.IOException;
//import java.net.URL;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//
//// ... Uncomment and use these three when the real backend is ready ...
//// import com.yamyam.messenger.client.network.NetworkService;
//// import com.yamyam.messenger.client.network.impl.NetworkChatServiceAdapter;
//// import com.yamyam.messenger.client.network.impl.NetworkContactServiceAdapter;
//
//import com.yamyam.messenger.client.util.ServiceLocator;
//import com.yamyam.messenger.client.network.impl.MockContactService;
//import com.yamyam.messenger.client.network.impl.MockChatService;
//
//public class TelegramClientApp extends Application {
//
//    private static Scene scene;                                // Global scene for the application
//    private static StackPane appRoot;                          // Root container used to switch views
//    private static final Map<String, Parent> cache = new HashMap<>(); // Cache for loaded FXML views
//
//    @Override
//    public void start(Stage stage) throws IOException {
//        // 1) Build the root and the main scene
//        appRoot = new StackPane();
//        scene = new Scene(appRoot, 1000, 700);
//
//        // 2) Add global stylesheets (scoped safely with .themed and .chat-root)
//        addStylesheetIfMissing(scene, "/com/yamyam/messenger/client/gui/styles/style.css");
//        addStylesheetIfMissing(scene, "/com/yamyam/messenger/client/gui/styles/chat.css");
//
//        // 3) Remove the initial white flash
//        scene.setFill(Color.web("#0B0F1A"));
//
//        // 4) Apply the saved or default theme
//        ThemeManager.apply(scene, ThemeManager.current());
//
//        // 4.5) Wire services so the UI can talk to the backend
//        // ... Mock services for running the app without a server ...
//        ServiceLocator.set(new MockContactService());
//        ServiceLocator.set(new MockChatService());
//
//        // ... Real wiring; when backend is ready, replace the three lines above with these ...
//        // var net = NetworkService.getInstance();
//        // ServiceLocator.set(new NetworkContactServiceAdapter(net));
//        // ServiceLocator.set(new NetworkChatServiceAdapter(net));
//
//        // 5) Configure the stage (icon, title, etc.)
//        stage.getIcons().add(new Image("/com/yamyam/messenger/client/gui/images/icon.png"));
//        stage.setTitle("Yamyam");
//        stage.setResizable(false);
//        stage.setScene(scene);
//
//        // 6) Show the welcome screen first (no animation)
//        Parent welcome = loadView("main/welcome");
//        ensureThemeClasses("main/welcome", welcome);
//        showInstant(welcome);
//
//        // 7) Preload the chat view in the same scene (CSS/layout resolved but hidden)
//        preloadForNoFlash("chat/chat");
//
//        stage.show();
//    }
//
//    /** Public API used by controllers to switch the root view */
//    public static void setRoot(String fxmlSimpleName) throws IOException {
//        Parent view = cache.computeIfAbsent(fxmlSimpleName, TelegramClientApp::loadOrThrow);
//        ensureThemeClasses(fxmlSimpleName, view);
//        // View is already preloaded in this Scene, so switching won’t flash white
//        showWithFade(view);
//    }
//
//    /* ................. Navigation / Loading ................. */
//
//    private static Parent loadOrThrow(String fxmlSimpleName) {
//        try { return loadView(fxmlSimpleName); }
//        catch (RuntimeException e) { throw e; }
//    }
//
//    private static Parent loadView(String fxmlSimpleName) {
//        return cache.computeIfAbsent(fxmlSimpleName, key -> {
//            try {
//                URL url = TelegramClientApp.class.getResource(
//                        "/com/yamyam/messenger/client/gui/fxml/" + key + ".fxml");
//                FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(url, key + ".fxml not found"));
//                return loader.load();
//            } catch (IOException ex) {
//                throw new RuntimeException(ex);
//            }
//        });
//    }
//
//    /** Preload a view into the same Scene to avoid a white flash during navigation */
//    private static void preloadForNoFlash(String fxmlSimpleName) {
//        Parent view = loadView(fxmlSimpleName);
//        ensureThemeClasses(fxmlSimpleName, view);
//
//        // Add to the scene graph temporarily but keep it hidden/unmanaged
//        if (!appRoot.getChildren().contains(view)) {
//            view.setVisible(false);
//            view.setManaged(false);
//            appRoot.getChildren().add(view);
//        }
//
//        // Resolve CSS and layout now (since it's already in the same Scene)
//        ThemeManager.reapply(scene);
//        view.applyCss();
//        view.layout();
//
//        // Warm up rendering pipeline (best-effort; ignore failures)
//        try { view.snapshot(new javafx.scene.SnapshotParameters(), null); } catch (Throwable ignore) {}
//
//        // Keep it hidden until we actually show it
//        view.setVisible(false);
//        view.setManaged(false);
//    }
//
//    private static void ensureThemeClasses(String fxmlSimpleName, Parent root) {
//        var classes = root.getStyleClass();
//        if (!classes.contains("themed")) classes.add("themed");
//        // Extra class for chat views
//        if (fxmlSimpleName.toLowerCase().contains("chat") && !classes.contains("chat-root")) {
//            classes.add("chat-root");
//        }
//    }
//
//    private static void addStylesheetIfMissing(Scene s, String resourcePath) {
//        URL url = TelegramClientApp.class.getResource(resourcePath);
//        if (url != null) {
//            String css = url.toExternalForm();
//            if (!s.getStylesheets().contains(css)) s.getStylesheets().add(css);
//        }
//    }
//
//    /* ................. View Transitions ................. */
//
//    private static void showInstant(Parent view) {
//        ThemeManager.reapply(scene);
//        // Clear everything and show only the given view
//        appRoot.getChildren().setAll(view);
//        view.setVisible(true);
//        view.setManaged(true);
//        view.applyCss();
//        view.layout();
//    }
//
//    private static void showWithFade(Parent view) {
//        ThemeManager.reapply(scene);
//
//        // Add to root if not already present
//        if (!appRoot.getChildren().contains(view)) appRoot.getChildren().add(view);
//
//        // Hide all siblings except the target view
//        for (var n : appRoot.getChildren()) {
//            if (n != view) { n.setVisible(false); n.setManaged(false); }
//        }
//        view.setManaged(true);
//        view.setOpacity(0);
//        view.setVisible(true);
//        view.applyCss();
//        view.layout();
//
//        FadeTransition ft = new FadeTransition(Duration.millis(140), view);
//        ft.setToValue(1.0);
//        ft.play();
//    }
//
//    public static void main(String[] args) { launch(args); }
//}
//
