package com.yamyam.messenger.client.gui.controller.chat;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;


import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

public final class PhotoLightbox {
    private final Stage stage;
    private final StackPane root = new StackPane();
    private final ImageView large = new ImageView();
    private final Label caption = new Label();
    private final HBox film = new HBox(8);
    private final ScrollPane filmScroll = new ScrollPane(film);
    private final Button prev = new Button("‹");
    private final Button next = new Button("›");
    private final Button close = new Button("✕");
    private final Button deleteBtn = new Button("Delete");

    private final List<String> uris;
    private int index;
    private final Consumer<Integer> onDelete; // callback into caller when delete pressed
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    public PhotoLightbox(Window owner, List<String> imageUris, int startIndex, Consumer<Integer> onDelete) {
        this.uris = new ArrayList<>(Objects.requireNonNull(imageUris));
        this.index = Math.max(0, Math.min(startIndex, uris.size()-1));
        this.onDelete = onDelete;

        this.stage = new Stage(StageStyle.TRANSPARENT);
        if (owner != null) stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);

        buildUI();
        stage.setScene(new Scene(root, Color.TRANSPARENT));
        stage.getScene().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource(
                        "/com/yamyam/messenger/client/gui/styles/chat.css")).toExternalForm()
        );

        stage.getScene().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
            if (e.getCode() == KeyCode.RIGHT) go( +1 );
            if (e.getCode() == KeyCode.LEFT)  go( -1 );
            if (e.getCode() == KeyCode.DELETE) deleteCurrent();
        });

        updateUI();
    }

    private void buildUI() {
        root.getStyleClass().add("photo-lightbox-root");

        // arrows
        prev.getStyleClass().add("photo-lightbox-arrow");
        next.getStyleClass().add("photo-lightbox-arrow");
        prev.setOnAction(e -> go(-1));
        next.setOnAction(e -> go(+1));

        // close
        close.getStyleClass().add("photo-lightbox-close");
        close.setOnAction(e -> stage.close());

        // delete
        deleteBtn.getStyleClass().add("photo-lightbox-delete");
        deleteBtn.setOnAction(e -> deleteCurrent());

        // main image
        large.setPreserveRatio(true);
        large.getStyleClass().add("photo-lightbox-img");
        large.setSmooth(true);

        // center layout
        StackPane.setAlignment(prev, Pos.CENTER_LEFT);
        StackPane.setAlignment(next, Pos.CENTER_RIGHT);
        StackPane.setAlignment(close, Pos.TOP_RIGHT);

        // bottom bar: caption + spacer + delete + filmstrip
        caption.getStyleClass().add("photo-lightbox-caption");

        film.setAlignment(Pos.CENTER_LEFT);
        filmScroll.setFitToHeight(true);
        filmScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        filmScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        filmScroll.getStyleClass().add("photo-lightbox-film");

        VBox bottom = new VBox(
                new HBox(10, caption, new Pane(), deleteBtn),
                filmScroll
        );
        VBox.setVgrow(filmScroll, Priority.NEVER);
        bottom.setFillWidth(true);
        bottom.setPadding(new Insets(10));
        StackPane.setAlignment(bottom, Pos.BOTTOM_CENTER);

        // sizing
        root.widthProperty().addListener((o,ov,nv) -> {
            large.setFitWidth(Math.max(400, nv.doubleValue() - 200));
        });
        root.heightProperty().addListener((o,ov,nv) -> {
            large.setFitHeight(Math.max(300, nv.doubleValue() - 220));
        });

        // close when clicking on dark background (but not on content)
        root.setOnMouseClicked(e -> {
            Node t = e.getPickResult().getIntersectedNode();
            if (t == root) stage.close();
        });

        root.getChildren().addAll(large, prev, next, close, bottom);
    }

    private void buildFilmstrip() {
        film.getChildren().clear();
        for (int i = 0; i < uris.size(); i++) {
            String u = uris.get(i);
            ImageView t = new ImageView(new Image(u, 96, 96, true, true));
            t.setFitHeight(72);
            t.setPreserveRatio(true);
            StackPane wrap = new StackPane(t);
            wrap.getStyleClass().add("photo-lightbox-thumb");
            if (i == index) wrap.getStyleClass().add("selected");
            final int idx = i;
            wrap.setOnMouseClicked(e -> { index = idx; updateUI(); });
            film.getChildren().add(wrap);
        }
    }

    private void go(int delta) {
        if (uris.isEmpty()) return;
        index = (index + delta + uris.size()) % uris.size();
        updateUI();
    }

    private void deleteCurrent() {
        if (uris.isEmpty()) return;
        if (onDelete != null) onDelete.accept(index);
        uris.remove(index);
        if (uris.isEmpty()) {
            stage.close();
            return;
        }
        if (index >= uris.size()) index = uris.size()-1;
        updateUI();
    }

    private void updateUI() {
        if (uris.isEmpty()) return;
        String u = uris.get(index);
        large.setImage(new Image(u, true));
        buildFilmstrip();

        String date = "";
        try {
            if (u != null) {
                URI uri = URI.create(u);
                if ("file".equalsIgnoreCase(uri.getScheme())) {
                    Path p = Paths.get(uri);
                    var lm = Files.getLastModifiedTime(p).toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                    date = " - " + df.format(lm);
                }
            }
        } catch (Exception ignored) { }

        caption.setText("Photo " + (index+1) + " of " + uris.size() + date);
    }

    public int showAndWait() {
        stage.setWidth(Math.max(900, Screen.getPrimary().getBounds().getWidth() * 0.86));
        stage.setHeight(Math.max(600, Screen.getPrimary().getBounds().getHeight() * 0.86));
        stage.centerOnScreen();
        stage.showAndWait();
        return index;
    }

    public static int open(Window owner, List<String> uris, int startIndex, Consumer<Integer> onDelete) {
        return new PhotoLightbox(owner, uris, startIndex, onDelete).showAndWait();
    }
}
