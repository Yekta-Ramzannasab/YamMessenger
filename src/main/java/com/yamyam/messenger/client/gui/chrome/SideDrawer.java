package com.yamyam.messenger.client.gui.chrome;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.Objects;

/**
 * Lightweight side drawer implemented as a Popup.
 * - Does NOT modify the main scene layout tree (safe for backend & FXML).
 * - Has a scrim (non-transparent) so text below never "bleeds" through.
 * - Smooth slide/opacity animations; ESC and outside-click to close.
 * - Contains a "Themes" section that calls provided callbacks.
 */
public class SideDrawer {

    private final Scene scene;
    private final Popup popup = new Popup();
    private final StackPane overlay = new StackPane();   // overlay-root
    private final Region scrim = new Region();           // overlay-scrim
    private final VBox drawer = new VBox(8);             // overlay-drawer

    private final Runnable onLight, onDark, onAmoled;

    public SideDrawer(Scene scene, Runnable onLight, Runnable onDark, Runnable onAmoled) {
        this.scene = Objects.requireNonNull(scene);
        this.onLight = onLight;
        this.onDark = onDark;
        this.onAmoled = onAmoled;

        overlay.getStyleClass().add("overlay-root");
        scrim.getStyleClass().add("overlay-scrim");
        drawer.getStyleClass().add("overlay-drawer");

        drawer.setPadding(new Insets(12));
        drawer.setPrefWidth(300);

        // Compose content
        Node header = buildHeader();
        Node items  = buildItems();
        drawer.getChildren().addAll(header, items);

        overlay.getChildren().addAll(scrim, drawer);
        StackPane.setAlignment(drawer, Pos.CENTER_LEFT);

        // Bind overlay size to scene
        overlay.setPrefSize(scene.getWidth(), scene.getHeight());
        scene.widthProperty().addListener((o, a, b) -> overlay.setPrefWidth(b.doubleValue()));
        scene.heightProperty().addListener((o, a, b) -> overlay.setPrefHeight(b.doubleValue()));

        // Behavior: click outside → close
        scrim.setOnMouseClicked(e -> hide());

        // Behavior: ESC → close
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (isShowing() && e.getCode() == KeyCode.ESCAPE) {
                hide();
                e.consume();
            }
        });

        // Non-transparent scrim so underlying content never pollutes the drawer
        scrim.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        scrim.setOpacity(0);
        drawer.setTranslateX(-340);

        popup.setAutoHide(false);
        popup.getContent().add(overlay);
    }

    private Node buildHeader() {
        Label title = new Label("Yam");
        title.getStyleClass().add("drawer__title");

        Label subtitle = new Label("Folders & Settings");
        subtitle.getStyleClass().add("drawer__subtitle");

        VBox left = new VBox(0, title, subtitle);

        Button close = new Button("✕");
        close.getStyleClass().add("drawer__close");
        close.setOnAction(e -> hide());

        HBox box = new HBox(8, left, new Pane(), close);
        HBox.setHgrow(box.getChildren().get(1), Priority.ALWAYS);
        box.getStyleClass().add("drawer__header");
        return box;
    }

    private Node buildItems() {
        VBox list = new VBox(2);
        list.setFillWidth(true);

        // Core items (Calls is intentionally omitted as requested)
        list.getChildren().addAll(
                row("My Profile", null, () -> {/* TODO: hook later */}),
                row("New Group", null, () -> {/* TODO */}),
                row("New Channel", null, () -> {/* TODO */}),
                row("Contacts", null, () -> {/* TODO */}),
                row("Saved Messages", null, () -> {/* TODO */}),
                row("Settings", null, () -> {/* TODO */})
        );

        // Themes inline section
        TitledPane themesPane = new TitledPane();
        themesPane.setText("Themes");
        themesPane.setExpanded(false);

        ToggleGroup tg = new ToggleGroup();
        RadioButton rbLight  = new RadioButton("Light");  rbLight.setToggleGroup(tg);
        RadioButton rbDark   = new RadioButton("Dark");   rbDark.setToggleGroup(tg);
        RadioButton rbAmoled = new RadioButton("AMOLED"); rbAmoled.setToggleGroup(tg);

        rbLight.setOnAction(e -> { if (onLight != null)  onLight.run(); });
        rbDark.setOnAction(e  -> { if (onDark  != null)  onDark.run();  });
        rbAmoled.setOnAction(e-> { if (onAmoled!= null)  onAmoled.run();});

        VBox themeBox = new VBox(8, rbLight, rbDark, rbAmoled);
        themeBox.setPadding(new Insets(6, 0, 6, 6));
        themesPane.setContent(themeBox);

        list.getChildren().add(themesPane);

        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return sp;
    }

    private HBox row(String title, String sub, Runnable action) {
        Label t = new Label(title); t.getStyleClass().add("folder-row__title");
        Label s = sub == null ? new Label("") : new Label(sub);
        s.getStyleClass().add("folder-row__sub");

        VBox labels = new VBox(2, t, s);
        labels.setFillWidth(true);

        Button more = new Button("⋯"); // placeholder till we wire actions
        more.getStyleClass().add("folder-row__trash");
        more.setVisible(false);

        HBox row = new HBox(10, labels, new Pane(), more);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        row.getStyleClass().add("folder-row");
        row.setOnMouseClicked(e -> { if (action != null) action.run(); });
        return row;
    }

    public boolean isShowing() { return popup.isShowing(); }

    public void show() {
        if (popup.isShowing()) return;
        popup.show(scene.getWindow());

        Timeline fade = new Timeline(new KeyFrame(Duration.millis(160),
                new KeyValue(scrim.opacityProperty(), 1.0, Interpolator.EASE_BOTH)));

        var slide = new javafx.animation.TranslateTransition(Duration.millis(220), drawer);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        fade.play();
        slide.play();
    }

    public void hide() {
        if (!popup.isShowing()) return;

        Timeline fade = new Timeline(new KeyFrame(Duration.millis(160),
                new KeyValue(scrim.opacityProperty(), 0.0, Interpolator.EASE_BOTH)));

        var slide = new javafx.animation.TranslateTransition(Duration.millis(220), drawer);
        slide.setToX(-340);
        slide.setInterpolator(Interpolator.EASE_BOTH);
        slide.setOnFinished(e -> popup.hide());

        fade.play();
        slide.play();
    }

    public void toggle() { if (isShowing()) hide(); else show(); }

    /** Lightweight hamburger graphic (three lines) */
    public static Node hamburgerGraphic() {
        VBox g = new VBox(4);
        for (int i = 0; i < 3; i++) {
            Rectangle r = new Rectangle(18, 2.2);
            r.setArcWidth(2.2);
            r.setArcHeight(2.2);
            r.getStyleClass().add("hamburger-line");
            g.getChildren().add(r);
        }
        g.setAlignment(Pos.CENTER_LEFT);
        return g;
    }
}
