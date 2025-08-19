package com.yamyam.messenger.client.gui.controller.chat;

import com.yamyam.messenger.client.gui.theme.ThemeManager;
import com.yamyam.messenger.client.util.AppSession;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ListView<ChatItem> chatList;
    @FXML private ListView<Msg> messageList;
    @FXML private TextArea inputField;
    @FXML private Button sendBtn;

    @FXML private ImageView headerAvatar;
    @FXML private Label headerName, headerStatus;

    @FXML private VBox infoPane;
    @FXML private ImageView infoAvatar;
    @FXML private Label infoName, infoPresence;
    @FXML private FlowPane mediaGrid;

    @FXML private MenuButton themeBtn;
    @FXML private RadioMenuItem miLight, miDark, miAmoled;


    private final ObservableList<ChatItem> allChats = FXCollections.observableArrayList();
    private final Image placeholder = new Image(
            Objects.requireNonNull(getClass().getResource(
                    "/com/yamyam/messenger/client/gui/images/profileDefault.jpg")).toExternalForm()
    );
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupChatList();
        setupMessageList();
        setupComposer();
        loadMockData();

        if (!AppSession.isLoggedIn()) {
            Platform.runLater(() -> {
                Scene scene = headerName.getScene();
                if (scene == null) return;

                var urlCss = getClass().getResource("/com/yamyam/messenger/client/gui/styles/chat.css");
                if (urlCss != null) {
                    String chatCss = urlCss.toExternalForm();
                    if (!scene.getStylesheets().contains(chatCss)) scene.getStylesheets().add(chatCss);
                }
                var root = scene.getRoot();
                var classes = root.getStyleClass();
                if (!classes.contains("themed")) classes.add("themed");
                if (!classes.contains("chat-root")) classes.add("chat-root");

                ThemeManager.reapply(scene);
                syncThemeMenu(ThemeManager.current());
            });
            return;
        }



        // Select the first chat by default (so the middle area isn't empty)
        if (!allChats.isEmpty()) {
            chatList.getSelectionModel().select(0);
            openChat(chatList.getSelectionModel().getSelectedItem());
        }

        // Ensure CSS and style classes are properly applied, then reapply theme
        Platform.runLater(() -> {
            Scene scene = headerName.getScene();
            if (scene == null) return;

            // Add chat.css if not already present
            var urlCss = getClass().getResource("/com/yamyam/messenger/client/gui/styles/chat.css");
            if (urlCss != null) {
                String chatCss = urlCss.toExternalForm();
                if (!scene.getStylesheets().contains(chatCss)) {
                    scene.getStylesheets().add(chatCss);
                }
            }

            // Ensure root has theme classes
            var root = scene.getRoot();
            var classes = root.getStyleClass();
            if (!classes.contains("themed")) classes.add("themed");
            if (!classes.contains("chat-root")) classes.add("chat-root");

            // Reapply the active theme and sync the menu selection
            ThemeManager.reapply(scene);
            syncThemeMenu(ThemeManager.current());
        });
    }

    /* ........... Chat list .......... */

    private void setupChatList() {
        chatList.setCellFactory(new Callback<>() {
            @Override public ListCell<ChatItem> call(ListView<ChatItem> lv) {
                return new ListCell<>() {
                    private final ImageView avatar = new ImageView();
                    private final Label name = new Label();
                    private final Label last = new Label();
                    private final VBox labels = new VBox(name, last);
                    private final HBox root = new HBox(10, avatar, labels);
                    {
                        root.setAlignment(Pos.CENTER_LEFT);
                        root.setPadding(new Insets(10,12,10,12));
                        avatar.setFitWidth(40); avatar.setFitHeight(40); avatar.setPreserveRatio(true);
                        name.getStyleClass().add("chat-conv__name");
                        last.getStyleClass().add("chat-conv__status");
                    }
                    @Override protected void updateItem(ChatItem it, boolean empty) {
                        super.updateItem(it, empty);
                        if (empty || it==null) { setGraphic(null); return; }
                        avatar.setImage(it.avatar!=null? it.avatar: placeholder);
                        name.setText(it.title + (it.online? " • Online": ""));
                        last.setText(it.lastMessagePreview());
                        setGraphic(root);
                    }
                };
            }
        });

        // Search filter
        searchField.textProperty().addListener((o,ov,nv)->{
            String q = nv==null? "": nv.trim().toLowerCase(Locale.ROOT);
            chatList.setItems(q.isEmpty()? allChats :
                    allChats.filtered(c-> c.title.toLowerCase(Locale.ROOT).contains(q)
                            || c.lastMessagePreview().toLowerCase(Locale.ROOT).contains(q)));
        });

        // Handle chat selection changes
        chatList.getSelectionModel().selectedItemProperty().addListener((o,old,sel)->{
            if (sel!=null) openChat(sel);
        });
    }

    /* .................... Messages  .................... */

    private void setupMessageList() {
        messageList.setCellFactory(lv -> new ListCell<>() {
            private final Label bubble = new Label();
            private final HBox line = new HBox(bubble);
            {
                bubble.getStyleClass().add("bubble");
                bubble.setWrapText(true);
                // Responsive width for message bubbles
                bubble.maxWidthProperty().bind(messageList.widthProperty().subtract(120));
                setPadding(new Insets(6,12,6,12));
            }

            @Override protected void updateItem(Msg m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m==null) { setGraphic(null); return; }
                bubble.setText(m.text + "  " + timeFmt.format(m.at));
                bubble.getStyleClass().removeAll("me","other");
                bubble.getStyleClass().add(m.isMe? "me":"other");
                line.setAlignment(m.isMe? Pos.CENTER_RIGHT: Pos.CENTER_LEFT);
                line.setNodeOrientation(m.isMe? NodeOrientation.RIGHT_TO_LEFT: NodeOrientation.LEFT_TO_RIGHT);
                setGraphic(line);
            }
        });

        // Auto-scroll to latest message
        messageList.getItems().addListener((ListChangeListener<? super Msg>)
                c -> Platform.runLater(() ->
                        messageList.scrollTo(messageList.getItems().size()-1)));
    }

    /* .................... Composer .................... */

    private void setupComposer() {
        sendBtn.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> inputField.getText()==null || inputField.getText().trim().isEmpty(),
                        inputField.textProperty())
        );
        inputField.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> { if (!e.isShiftDown()) { e.consume(); sendMessage(); } }
            }
        });
    }

    /* .................... Actions .................... */

    @FXML private void sendMessage() {
        String text = Optional.ofNullable(inputField.getText()).orElse("").trim();
        if (text.isEmpty()) return;
        ChatItem sel = chatList.getSelectionModel().getSelectedItem();
        if (sel==null) return;

        sel.messages.add(new Msg(true, text, LocalDateTime.now()));
        inputField.clear();
        // Simulate reply
        Platform.runLater(() -> sel.messages.add(new Msg(false,"Got it ✅", LocalDateTime.now())));
    }

    /* ===== Theme menu handlers (connected via FXML) ===== */

    @FXML public void setThemeLight()  { switchTo(ThemeManager.Theme.LIGHT); }
    @FXML public void setThemeDark()   { switchTo(ThemeManager.Theme.DARK); }
    @FXML public void setThemeAmoled() { switchTo(ThemeManager.Theme.AMOLED); }

    // Cycle themes when clicking the menu button itself
    @FXML public void cycleTheme() {
        var cur = ThemeManager.current();
        var next = switch (cur) {
            case LIGHT  -> ThemeManager.Theme.DARK;
            case DARK   -> ThemeManager.Theme.AMOLED;
            case AMOLED -> ThemeManager.Theme.LIGHT;
        };
        switchTo(next);
    }
    private void switchTo(ThemeManager.Theme t) {
        var scene = headerName.getScene();
        if (scene == null) return;
        ThemeManager.apply(scene, t);
        ThemeManager.reapply(scene);
        syncThemeMenu(t);
    }

    private void syncThemeMenu(ThemeManager.Theme t) {
        if (miLight != null)  miLight.setSelected(t == ThemeManager.Theme.LIGHT);
        if (miDark  != null)  miDark.setSelected(t == ThemeManager.Theme.DARK);
        if (miAmoled!= null)  miAmoled.setSelected(t == ThemeManager.Theme.AMOLED);
        if (themeBtn!= null)  themeBtn.setText("Theme");
    }

    /* .................... Helpers ....................*/

    private void openChat(ChatItem c) {
        headerAvatar.setImage(c.avatar!=null? c.avatar: placeholder);
        headerName.setText(c.title);
        headerStatus.setText(c.online? "Online": "Last seen recently");

        infoAvatar.setImage(c.avatar!=null? c.avatar: placeholder);
        infoName.setText(c.title);
        infoPresence.setText(c.online? "Online": "Offline");

        messageList.setItems(c.messages);
    }

    // Mock chat data for testing UI
    private void loadMockData() {
        allChats.addAll(
                ChatItem.of("Caroline Gray", true, null,
                        List.of(new Msg(false,"Hi 👋", now(-15)), new Msg(true,"Hey!", now(-14)))),
                ChatItem.of("Presley Martin", false, null,
                        List.of(new Msg(false,"Design looks cool 🔥", now(-60)))),
                ChatItem.of("Matthew Brown", true, null,
                        List.of(new Msg(true,"Sending docs…", now(-120)), new Msg(false,"Thanks!", now(-118))))
        );
        chatList.setItems(allChats);
    }

    private static LocalDateTime now(int m){ return LocalDateTime.now().minusMinutes(m); }

    /* Mini data models used by the controller */
    public static final class ChatItem {
        public final String title; public final boolean online; public final Image avatar;
        public final ObservableList<Msg> messages = FXCollections.observableArrayList();
        private ChatItem(String t, boolean o, Image a){ title=t; online=o; avatar=a; }
        public static ChatItem of(String t, boolean o, Image a, List<Msg> msgs){ var c=new ChatItem(t,o,a); c.messages.addAll(msgs); return c; }
        public String lastMessagePreview(){ if(messages.isEmpty()) return ""; String t=messages.get(messages.size()-1).text; return t.length()>30? t.substring(0,30)+"…": t; }
    }
    public static final class Msg { public final boolean isMe; public final String text; public final LocalDateTime at;
        public Msg(boolean m,String t,LocalDateTime a){ isMe=m; text=t; at=a; } }
}
