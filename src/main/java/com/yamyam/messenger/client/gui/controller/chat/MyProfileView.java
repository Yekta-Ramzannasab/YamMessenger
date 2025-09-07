package com.yamyam.messenger.client.gui.controller.chat;

import com.yamyam.messenger.client.util.AppSession;
import com.yamyam.messenger.shared.model.user.UserProfile;
import com.yamyam.messenger.shared.model.user.Users;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.nio.file.*;
import java.util.prefs.Preferences;

/**
 * Self-contained view for "My Profile".
 * - No FXML. Builds a Node tree to inject into ChatController's subPageOverlay.
 * - Local persistence via Preferences until backend is wired.
 * - Exposes small API: getRoot(), enterEdit(), cancel(), isEditing().
 */
public final class MyProfileView {

    /* ---------- State ---------- */

    private final Preferences prefs = Preferences.userRoot().node("com.yamyam.messenger.client.profile");
    private final Image placeholder;

    private final VBox root = new VBox(12);               // whole card
    private final ImageView avatar = new ImageView();     // circular avatar
    private final Hyperlink linkChangePhoto = new Hyperlink("Change photo");

    private final TextField tfName = new TextField();
    private final TextField tfUsername = new TextField();
    private final TextArea  taBio = new TextArea();
    private final Label     lblEmail = new Label();

    private final HBox actions = new HBox(10);
    private final Button btnSave = new Button("Save");
    private final Button btnCancel = new Button("Cancel");

    private boolean editing = false;
    private String pendingImagePath = null;

    /* ---------- API ---------- */

    public MyProfileView(Image placeholder) {
        this.placeholder = placeholder;
        buildUI();
        loadFromSessionAndPrefs();
        applyEditState(false);
    }

    public Node getRoot() { return root; }

    public boolean isEditing() { return editing; }

    /** called by top "Edit" button */
    public void enterEdit() {
        if (editing) return;
        applyEditState(true);
    }

    /** called when overlay closes or user taps Cancel top/back */
    public void cancel() {
        // revert to last persisted values
        loadFromSessionAndPrefs();
        pendingImagePath = null;
        applyEditState(false);
    }

    /* ---------- Internals ---------- */

    private void buildUI() {
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(8, 0, 0, 0));
        root.getStyleClass().add("profile-card");

        // title (only once)
        Label title = new Label("My Profile");
        title.getStyleClass().add("profile-title");

        // avatar (circular)
        avatar.setFitWidth(96); avatar.setFitHeight(96); avatar.setPreserveRatio(true);
        Circle clip = new Circle(48, 48, 48);
        avatar.setClip(clip);
        avatar.fitWidthProperty().addListener((o, ov, nv) -> clip.setCenterX(nv.doubleValue() / 2));
        avatar.fitHeightProperty().addListener((o, ov, nv) -> clip.setCenterY(nv.doubleValue() / 2));
        avatar.getStyleClass().add("profile-avatar");

        linkChangePhoto.getStyleClass().add("profile-link");
        linkChangePhoto.setOnAction(e -> {
            if (!editing) return;
            choosePhoto();
        });

        // Grid form: like Telegram – labels at left, underline inputs at right
        GridPane form = new GridPane();
        form.setMaxWidth(420);
        form.setHgap(12);
        form.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(28);
        c1.setHalignment(HPos.RIGHT);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(72);
        form.getColumnConstraints().addAll(c1, c2);

        // style inputs
        tfName.getStyleClass().addAll("profile-input", "profile-underline");
        tfUsername.getStyleClass().addAll("profile-input", "profile-underline");
        taBio.getStyleClass().addAll("profile-input", "profile-underline");
        taBio.setPrefRowCount(3);
        taBio.setWrapText(true);

        form.addRow(0, new Label("Name:"), tfName);
        form.addRow(1, new Label("Username:"), tfUsername);
        form.addRow(2, new Label("Email:"), lblEmail);
        form.addRow(3, new Label("Bio:"), taBio);

        // bottom actions (hidden until edit)
        btnSave.getStyleClass().add("profile-primary");
        btnCancel.getStyleClass().add("profile-secondary");

        btnSave.setOnAction(e -> {
            normalizeUsername();
            persistToPrefs();
            // TODO: wire backend here (updateProfile(...)) – deliberately left out to avoid breaking build.
            applyEditState(false);
            pendingImagePath = null;
        });

        btnCancel.setOnAction(e -> cancel());

        actions.getChildren().setAll(btnSave, btnCancel);
        actions.setAlignment(Pos.CENTER);
        actions.getStyleClass().add("profile-actions");

        // assemble
        root.getChildren().setAll(
                title,
                avatar,
                linkChangePhoto,
                form,
                actions
        );
    }

    private void applyEditState(boolean on) {
        editing = on;

        // inputs
        tfName.setEditable(on);
        tfUsername.setEditable(on);
        taBio.setEditable(on);

        // class for readonly style
        toggleStyle(tfName, "profile-readonly", !on);
        toggleStyle(tfUsername, "profile-readonly", !on);
        toggleStyle(taBio, "profile-readonly", !on);

        // actions visibility
        actions.setManaged(on);
        actions.setVisible(on);
    }

    private void toggleStyle(Control c, String cls, boolean add) {
        if (add) { if (!c.getStyleClass().contains(cls)) c.getStyleClass().add(cls); }
        else c.getStyleClass().remove(cls);
    }

    private void loadFromSessionAndPrefs() {
        Users me = AppSession.getCurrentUser();
        UserProfile p = (me != null) ? me.getUserProfile() : null;

        // values priority: prefs -> session -> ""
        String name = prefs.get("name", p != null && p.getProfileName() != null ? p.getProfileName() : "");
        String uname = prefs.get("username", p != null && p.getUsername() != null ? p.getUsername() : "");
        String bio = prefs.get("bio", p != null && p.getBio() != null ? p.getBio() : "");
        String email = (me != null && me.getEmail() != null) ? me.getEmail() : "—";

        tfName.setText(name);
        tfUsername.setText(uname);
        taBio.setText(bio);
        lblEmail.setText(email);

        // avatar: prefs path -> profile url -> placeholder
        String saved = prefs.get("imagePath", null);
        if (saved != null && !saved.isBlank() && Files.exists(Path.of(saved))) {
            avatar.setImage(new Image(Path.of(saved).toUri().toString(), true));
        } else if (p != null && p.getProfileImageUrl() != null && !p.getProfileImageUrl().isBlank()) {
            avatar.setImage(new Image(p.getProfileImageUrl(), true));
        } else {
            avatar.setImage(placeholder);
        }
    }

    private void persistToPrefs() {
        prefs.put("name", safe(tfName.getText()));
        prefs.put("username", safe(tfUsername.getText()));
        prefs.put("bio", safe(taBio.getText()));
        if (pendingImagePath != null) {
            prefs.put("imagePath", pendingImagePath);
        }
    }

    private void choosePhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose profile photo");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        var win = root.getScene() != null ? root.getScene().getWindow() : null;
        var file = fc.showOpenDialog(win);
        if (file == null) return;

        try {
            Path appDir = Path.of(System.getProperty("user.home"), ".yam", "profile");
            Files.createDirectories(appDir);
            String fileName = "avatar_" + System.currentTimeMillis() + getExt(file.getName());
            Path dest = appDir.resolve(fileName);
            Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            pendingImagePath = dest.toString();
            avatar.setImage(new Image(dest.toUri().toString(), true));

        } catch (Exception ex) {
            ex.printStackTrace();
            //  show toast with alert
        }
    }

    private void normalizeUsername() {
        String u = safe(tfUsername.getText()).replace("@", "").trim();
        tfUsername.setText(u);
    }

    private static String safe(String s){ return s == null ? "" : s; }
    private static String getExt(String n){ int i = n.lastIndexOf('.'); return i >= 0 ? n.substring(i) : ""; }
}
