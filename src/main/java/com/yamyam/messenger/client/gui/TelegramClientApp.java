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

// ===== wiring (DEV: use mocks) =====
import com.yamyam.messenger.client.util.ServiceLocator;
// --- اگر خواستی به بک واقعی وصل شوی، این سه خط را از کامنت دربیار و پایین موک‌ها را کامنت کن ---
// import com.yamyam.messenger.client.network.NetworkService;
// import com.yamyam.messenger.client.network.impl.NetworkContactServiceAdapter;
// import com.yamyam.messenger.client.network.impl.NetworkChatServiceAdapter;

import com.yamyam.messenger.client.network.impl.MockContactService;
import com.yamyam.messenger.client.network.impl.MockChatService;

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
        // --- واقعی (وقتی بک حاضر شد) ---
//        var net = NetworkService.getInstance();
//        ServiceLocator.set(new NetworkContactServiceAdapter(net));
//        ServiceLocator.set(new NetworkChatServiceAdapter(net));

        // --- موک برای توسعه‌ی فرانت ---
        ServiceLocator.set(new MockContactService());
        ServiceLocator.set(new MockChatService());

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


