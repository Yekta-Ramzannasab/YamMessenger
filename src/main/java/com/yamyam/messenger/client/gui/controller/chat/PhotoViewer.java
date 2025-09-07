package com.yamyam.messenger.client.gui.controller.chat;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URI;
import java.nio.file.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class PhotoViewer {
    private PhotoViewer(){}

    public static void show(List<String> imageUris) { show(imageUris, 0); }

    public static void show(List<String> imageUris, int startIndex) {
        if (imageUris == null || imageUris.isEmpty()) return;

        final List<String> uris = new ArrayList<>(imageUris);
        final int[] idx = { Math.max(0, Math.min(startIndex, uris.size()-1)) };

        // ----- core nodes
        ImageView iv = new ImageView(); // main image
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        // thumbs rail (centered)
        HBox thumbs = new HBox(8);
        thumbs.getStyleClass().add("pv-thumbs");
        thumbs.setAlignment(Pos.CENTER);

        // footer info + delete
        Label info = new Label();
        info.getStyleClass().add("pv-info");
        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("pv-delete");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(12, info, spacer, btnDelete);
        footer.setAlignment(Pos.CENTER_LEFT);

        // arrows in the bottom bar (not on the image)
        Button btnPrev = new Button("❮");
        Button btnNext = new Button("❯");
        btnPrev.getStyleClass().add("pv-arrow");
        btnNext.getStyleClass().add("pv-arrow");

        HBox rail = new HBox(12, btnPrev, thumbs, btnNext);
        rail.setAlignment(Pos.CENTER);

        VBox bar = new VBox(10, rail, footer);
        bar.getStyleClass().add("pv-bar");

        BorderPane chrome = new BorderPane();
        chrome.setCenter(new StackPane(iv));
        chrome.setBottom(bar);
        chrome.getStyleClass().add("pv-pane");

        // close button
        Button btnClose = new Button("✕");
        btnClose.getStyleClass().add("pv-close");
        StackPane closeWrap = new StackPane(btnClose);
        StackPane.setAlignment(btnClose, Pos.TOP_RIGHT);
        chrome.setTop(closeWrap);

        // scrim + stage
        StackPane root = new StackPane();
        Region scrim = new Region();
        scrim.getStyleClass().add("pv-scrim");
        root.getChildren().addAll(scrim, chrome);
        StackPane.setAlignment(chrome, Pos.CENTER);

        var screen = Screen.getPrimary().getVisualBounds();
        double sceneW = screen.getWidth();
        double sceneH = screen.getHeight();

        Scene scene = new Scene(root, sceneW, sceneH, Color.TRANSPARENT);
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);

        // css
        String css = Objects.requireNonNull(
                PhotoViewer.class.getResource("/com/yamyam/messenger/client/gui/styles/chat.css")
        ).toExternalForm();
        scene.getStylesheets().add(css);

        // size limits (panel sized relative to screen; image always a bit smaller)
        double maxW = Math.min(1100, sceneW * 0.72);
        double maxH = Math.min(820,  sceneH * 0.82);
        double sidePad = 32;          // pane inner padding
        double arrowsSpace = 0;       // arrows are in bar, not overlaying image
        double barReserve = 190;      // space for thumbs + footer

        chrome.setMaxWidth(maxW);
        chrome.setMaxHeight(maxH);

        // fit image as chrome resizes
        chrome.widthProperty().addListener((o,ov,nv) ->
                iv.setFitWidth(nv.doubleValue() - (sidePad*2) - arrowsSpace));
        chrome.heightProperty().addListener((o,ov,nv) ->
                iv.setFitHeight(Math.max(240, nv.doubleValue() - barReserve)));

        // interactions
        scrim.setOnMouseClicked(e -> stage.close());
        btnClose.setOnAction(e -> stage.close());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
            if (e.getCode() == KeyCode.RIGHT) { idx[0] = (idx[0]+1) % uris.size(); update(iv, info, thumbs, uris, idx[0]); }
            if (e.getCode() == KeyCode.LEFT)  { idx[0] = (idx[0]-1+uris.size()) % uris.size(); update(iv, info, thumbs, uris, idx[0]); }
        });
        btnPrev.setOnAction(e -> { idx[0] = (idx[0]-1+uris.size()) % uris.size(); update(iv, info, thumbs, uris, idx[0]); });
        btnNext.setOnAction(e -> { idx[0] = (idx[0]+1) % uris.size(); update(iv, info, thumbs, uris, idx[0]); });

        // delete only if local file
        btnDelete.setOnAction(e -> {
            String u = uris.get(idx[0]);
            if (!u.startsWith("file:")) return;
            try {
                Path p = Paths.get(URI.create(u));
                Files.deleteIfExists(p);
            } catch (Exception ignored) {}
            uris.remove(idx[0]);
            if (uris.isEmpty()) { stage.close(); return; }
            idx[0] = Math.min(idx[0], uris.size()-1);
            rebuildThumbs(thumbs, uris, iv, info, idx);
        });

        // initial build and show
        rebuildThumbs(thumbs, uris, iv, info, idx);
        // make sure first fit is applied using current size
        iv.setFitWidth(chrome.getMaxWidth() - (sidePad*2) - arrowsSpace);
        iv.setFitHeight(Math.max(240, chrome.getMaxHeight() - barReserve));

        stage.show();
    }

    private static void rebuildThumbs(HBox thumbs, List<String> uris, ImageView iv, Label info, int[] idx){
        thumbs.getChildren().clear();
        for (int i = 0; i < uris.size(); i++) {
            final int ti = i;
            ImageView tiv = new ImageView(new Image(uris.get(i), 72, 72, true, true));
            tiv.setFitWidth(72); tiv.setFitHeight(72); tiv.setPreserveRatio(true);
            StackPane cell = new StackPane(tiv);
            cell.getStyleClass().add("pv-thumb");
            cell.setOnMouseClicked(ev -> { idx[0] = ti; update(iv, info, thumbs, uris, idx[0]); });
            thumbs.getChildren().add(cell);
        }
        update(iv, info, thumbs, uris, idx[0]);
    }

    private static void update(ImageView iv, Label info, HBox thumbs, List<String> uris, int index){
        String u = uris.get(index);
        iv.setImage(new Image(u, 1600, 1200, true, true));

        for (int i = 0; i < thumbs.getChildren().size(); i++) {
            Node n = thumbs.getChildren().get(i);
            n.getStyleClass().remove("pv-thumb--active");
            if (i == index) n.getStyleClass().add("pv-thumb--active");
        }

        String date = "";
        try {
            if (u.startsWith("file:")) {
                Path p = Paths.get(URI.create(u));
                var lm = Files.getLastModifiedTime(p).toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime();
                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
                date = " — " + df.format(lm);
            }
        } catch (Exception ignored) {}
        info.setText("Photo " + (index + 1) + " of " + uris.size() + date);
    }
}

