package com.yamyam.messenger.client.gui.controller.chat;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;

import com.yamyam.messenger.client.gui.theme.ThemeManager;
import com.yamyam.messenger.client.network.service.ContactService;
import com.yamyam.messenger.client.network.dto.Contact;
import com.yamyam.messenger.client.util.AppSession;
import com.yamyam.messenger.client.util.ServiceLocator;
import com.yamyam.messenger.shared.model.Users;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
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

    /* -----* *------
       FXML references (wired from .fxml)
       -----* *------ */
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

    // Drawer / overlay (Folders)
    @FXML private StackPane foldersOverlay;
    @FXML private Pane overlayScrim;
    @FXML private VBox foldersDrawer;
    @FXML private ListView<FolderVM> foldersList;
    @FXML private Button btnCreateFolder, btnHamburger;

    /* -----* *------
       Controller state & formatting
       -----* *------ */
    private final ObservableList<ChatItem> allChats = FXCollections.observableArrayList();
    private final FilteredList<ChatItem> filteredChats = new FilteredList<>(allChats, c -> true);

    private final Image placeholder = new Image(
            Objects.requireNonNull(getClass().getResource(
                    "/com/yamyam/messenger/client/gui/images/profileDefault.jpg")).toExternalForm()
    );

    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!AppSession.isLoggedIn()) {
            System.err.println("❌ No user in session — redirecting or blocking UI");
            return;
        }

        Users me = AppSession.getCurrentUser();
        System.out.println("✅ Logged in as: " + me.getEmail());

        setupChatList();
        setupMessageList();
        setupComposer();

        loadContactsFromService();

        if (!allChats.isEmpty()) {
            chatList.getSelectionModel().select(0);
            openChat(chatList.getSelectionModel().getSelectedItem());
        } else {
            System.out.println("ℹ️ No chats available for user: " + me.getEmail());
        }

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

            // NEW
            hookCombinedFiltering();   // search+folder unified
            setupFoldersDrawer();      // drawer cell factory + clicks
            refreshFoldersList();      // counts
            closeFoldersDrawer();      // start hidden
        });
    }

    /* ================== Chat list ================== */
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
                        avatar.setFitWidth(40);
                        avatar.setFitHeight(40);
                        avatar.setPreserveRatio(true);
                        name.getStyleClass().add("chat-conv__name");
                        last.getStyleClass().add("chat-conv__status");
                    }
                    @Override protected void updateItem(ChatItem it, boolean empty) {
                        super.updateItem(it, empty);
                        if (empty || it==null) { setGraphic(null); return; }
                        avatar.setImage(it.avatar != null ? it.avatar : placeholder);

                        String typeTag = switch (it.kind) {
                            case DIRECT -> "";
                            case GROUP -> "  • Group";
                            case CHANNEL -> "  • Channel";
                        };
                        String presence = (it.kind == ChatKind.DIRECT && it.online) ? " • Online" : "";
                        name.setText(it.title + typeTag + presence);

                        last.setText(it.lastMessagePreview());
                        setGraphic(root);
                    }
                };
            }
        });

        // IMPORTANT: the list uses the unified filtered view
        chatList.setItems(filteredChats);

        chatList.getSelectionModel().selectedItemProperty().addListener((o, oldSel, sel) -> {
            if (sel == null) return;
            openChat(sel);
            long meUserId = AppSession.isLoggedIn() ? AppSession.requireUserId() : 1L;
            try { ServiceLocator.chat().openChat(meUserId, sel.contactId); }
            catch (Exception ex) { System.err.println("[chat] openChat failed: " + ex.getMessage()); }
        });
    }

    /* ================== Messages ================== */
    private void setupMessageList() {
        messageList.setCellFactory(lv -> new ListCell<>() {
            private final Label bubble = new Label();
            private final HBox line = new HBox(bubble);
            {
                bubble.getStyleClass().add("bubble");
                bubble.setWrapText(true);
                bubble.maxWidthProperty().bind(messageList.widthProperty().subtract(120));
                setPadding(new Insets(6,12,6,12));
            }
            @Override protected void updateItem(Msg m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) { setGraphic(null); return; }
                bubble.setText(m.text + "  " + timeFmt.format(m.at));
                bubble.getStyleClass().removeAll("me", "other");
                bubble.getStyleClass().add(m.isMe ? "me" : "other");
                line.setAlignment(m.isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                line.setNodeOrientation(m.isMe ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
                setGraphic(line);
            }
        });

        messageList.getItems().addListener((ListChangeListener<? super Msg>) c ->
                Platform.runLater(() -> messageList.scrollTo(messageList.getItems().size() - 1)));
    }

    /* ================== Composer ================== */
    private void setupComposer() {
        sendBtn.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> inputField.getText() == null || inputField.getText().trim().isEmpty(),
                        inputField.textProperty())
        );
        inputField.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> { if (!e.isShiftDown()) { e.consume(); sendMessage(); } }
            }
        });
    }

    @FXML
    private void sendMessage() {
        String text = Optional.ofNullable(inputField.getText()).orElse("").trim();
        if (text.isEmpty()) return;

        ChatItem selectedChat = chatList.getSelectionModel().getSelectedItem();
        if (selectedChat == null) return;

        // try { ServiceLocator.chat().sendMessage(selectedChat.contactId, text); } catch (Exception ignored) {}
        inputField.clear();
    }

    /* ================== Theme ================== */
    @FXML public void setThemeLight()  { switchTo(ThemeManager.Theme.LIGHT); }
    @FXML public void setThemeDark()   { switchTo(ThemeManager.Theme.DARK); }
    @FXML public void setThemeAmoled() { switchTo(ThemeManager.Theme.AMOLED); }

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
        if (miLight  != null) miLight.setSelected(t == ThemeManager.Theme.LIGHT);
        if (miDark   != null) miDark.setSelected(t == ThemeManager.Theme.DARK);
        if (miAmoled != null) miAmoled.setSelected(t == ThemeManager.Theme.AMOLED);
        if (themeBtn != null) themeBtn.setText("Theme");
    }

    /* ================== Public loading APIs ================== */
    public void loadChats(List<Contact> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            loadChatsGeneric(Collections.emptyList());
            return;
        }

        List<ChatRef> refs = new ArrayList<>(contacts.size());
        for (Contact c : contacts) {
            ChatKind kind = switch (c.type()) {
                case DIRECT -> ChatKind.DIRECT;
                case GROUP -> ChatKind.GROUP;
                case CHANNEL -> ChatKind.CHANNEL;
            };

            refs.add(new ChatRef(
                    c.id(),
                    kind,
                    c.title(),
                    c.avatarUrl(),
                    c.online(),
                    c.memberCount(),
                    false,
                    0
            ));
        }

        loadChatsGeneric(refs);
    }

    public void loadChatsGeneric(List<ChatRef> refs) {
        Long keepSelectedId = Optional.ofNullable(chatList.getSelectionModel().getSelectedItem())
                .map(ci -> ci.contactId).orElse(null);

        var safe = (refs == null) ? List.<ChatRef>of() : refs;
        var newItems = new ArrayList<ChatItem>(safe.size());
        for (ChatRef r : safe) {
            Image av = (r.avatarUrl != null && !r.avatarUrl.isBlank())
                    ? new Image(r.avatarUrl, true) : null;   // FIX: field access, not method
            newItems.add(ChatItem.fromRef(r, av));
        }

        allChats.setAll(newItems);
        chatList.setItems(filteredChats); // keep bound to filtered view

        if (!allChats.isEmpty()) {
            int idx = 0;
            if (keepSelectedId != null) {
                for (int i = 0; i < allChats.size(); i++) {
                    if (allChats.get(i).contactId == keepSelectedId) { idx = i; break; }
                }
            }
            chatList.getSelectionModel().select(idx);
            openChat(chatList.getSelectionModel().getSelectedItem());
        } else {
            messageList.setItems(FXCollections.observableArrayList());
            headerName.setText("");
            headerStatus.setText("");
            infoName.setText("");
            infoPresence.setText("");
            headerAvatar.setImage(placeholder);
            infoAvatar.setImage(placeholder);
        }

        refreshFoldersList();
        updateFilterPredicate();
    }

    /* ================== Helpers ================== */
    private void openChat(ChatItem c) {
        headerAvatar.setImage(c.avatar != null ? c.avatar : placeholder);
        headerName.setText(c.title);

        String status = switch (c.kind) {
            case DIRECT  -> (c.online ? "Online" : "Last seen recently");
            case GROUP   -> (c.memberCount != null ? c.memberCount + " members" : "Group");
            case CHANNEL -> "Channel";
        };
        headerStatus.setText(status);

        infoAvatar.setImage(c.avatar != null ? c.avatar : placeholder);
        infoName.setText(c.title);
        infoPresence.setText(status);

        messageList.setItems(c.messages);
    }

    private void loadContactsFromService() {
        ContactService contactService = ServiceLocator.contacts();

        new Thread(() -> {
            try {
                long meUserId = AppSession.requireUserId();
                final List<Contact> contacts = contactService.getContacts(meUserId);

                Platform.runLater(() -> {
                    allChats.clear();
                    for (Contact c : contacts) {
                        Image av = (c.avatarUrl() != null && !c.avatarUrl().isBlank())
                                ? new Image(c.avatarUrl(), true) : null;
                        allChats.add(ChatItem.fromContact(c, av));
                    }
                    refreshFoldersList();
                    updateFilterPredicate();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        new Alert(Alert.AlertType.ERROR, "Failed to load contacts.").showAndWait()
                );
            }
        }).start();
    }

    private static LocalDateTime now(int m){ return LocalDateTime.now().minusMinutes(m); }

    /* ================== Mini models (UI-only) ================== */
    public enum ChatKind { DIRECT, GROUP, CHANNEL }

    public static final class ChatRef {
        public final long id;
        public final ChatKind kind;
        public final String title;
        public final String avatarUrl;
        public final boolean online;
        public final Integer memberCount;
        public final boolean muted;
        public final int unreadCount;

        public ChatRef(long id, ChatKind kind, String title, String avatarUrl,
                       boolean online, Integer memberCount, boolean muted, int unreadCount) {
            this.id = id; this.kind = kind; this.title = title; this.avatarUrl = avatarUrl;
            this.online = online; this.memberCount = memberCount; this.muted = muted; this.unreadCount = unreadCount;
        }
    }

    public static final class ChatItem {
        public final long contactId;
        public final String title;
        public boolean online;
        public final Image avatar;
        public final ChatKind kind;
        public final Integer memberCount;
        public final boolean muted;
        public final ObservableList<Msg> messages = FXCollections.observableArrayList();

        private ChatItem(long id, String t, boolean o, Image a, ChatKind k, Integer memberCount, boolean muted) {
            contactId=id; title=t; online=o; avatar=a; kind=k; this.memberCount = memberCount; this.muted = muted;
        }

        public static ChatItem fromContact(Contact c, Image avatar) {
            return new ChatItem(c.id(), c.title(), c.online(), avatar, ChatKind.DIRECT, null, false);
        }

        public static ChatItem fromRef(ChatRef r, Image avatar) {
            boolean online = (r.kind == ChatKind.DIRECT) && r.online;
            return new ChatItem(r.id, r.title, online, avatar, r.kind, r.memberCount, r.muted);
        }

        public static ChatItem of(String t, boolean o, Image a, List<Msg> msgs){
            var c = new ChatItem(-1, t, o, a, ChatKind.DIRECT, null, false);
            c.messages.addAll(msgs); return c;
        }

        public String lastMessagePreview(){
            if (messages.isEmpty()) return "";
            String t = messages.get(messages.size()-1).text;
            return t.length()>30 ? t.substring(0,30)+"…" : t;
        }
    }

    public static final class Msg {
        public final boolean isMe;
        public final String text;
        public final LocalDateTime at;

        public Msg(boolean m, String t, LocalDateTime a){ isMe=m; text=t; at=a; }
    }

    // Folder view-model for the drawer list
    public static final class FolderVM {
        public final String name;
        public final int count;
        public final boolean deletable;
        public FolderVM(String n, int c, boolean d){
            this.name = n;
            this.count = c;
            this.deletable = d;
        }
    }

    /* ========= Filtering (search + folder) ========= */
    private enum ActiveFolder { ALL, CHANNELS, GROUPS, PV }
    private ActiveFolder activeFolder = ActiveFolder.ALL;

    private void hookCombinedFiltering() {
        chatList.setItems(filteredChats);
        searchField.textProperty().addListener((o, ov, nv) -> updateFilterPredicate());
        updateFilterPredicate();
    }

    private void updateFilterPredicate() {
        final String q = Optional.ofNullable(searchField.getText())
                .orElse("").trim().toLowerCase(Locale.ROOT);

        filteredChats.setPredicate(ci -> {
            boolean folderOk = switch (activeFolder) {
                case ALL      -> true;
                case CHANNELS -> ci.kind == ChatKind.CHANNEL;
                case GROUPS   -> ci.kind == ChatKind.GROUP;
                case PV       -> ci.kind == ChatKind.DIRECT;
            };
            boolean searchOk = q.isEmpty()
                    || ci.title.toLowerCase(Locale.ROOT).contains(q)
                    || ci.lastMessagePreview().toLowerCase(Locale.ROOT).contains(q);
            return folderOk && searchOk;
        });
    }

    /* ================== Folders Drawer ================== */
    private boolean foldersOpen = false;

    @FXML public void openFoldersManager() { openFoldersDrawer(); }

    private void setupFoldersDrawer() {
        if (foldersList == null) return;

        foldersList.setCellFactory(lv -> new ListCell<>() {
            private final Label icon  = new Label("📁");
            private final Label title = new Label();
            private final Label sub   = new Label();
            private final Button trash = new Button("🗑");
            private final VBox texts = new VBox(title, sub);
            private final Pane spacer = new Pane();
            private final HBox root = new HBox(10, icon, texts, spacer, trash);
            {
                root.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(spacer, Priority.ALWAYS);
                root.getStyleClass().add("folder-row");
                title.getStyleClass().add("folder-row__title");
                sub.getStyleClass().add("folder-row__sub");
                trash.getStyleClass().add("folder-row__trash");
            }
            @Override protected void updateItem(FolderVM vm, boolean empty) {
                super.updateItem(vm, empty);
                if (empty || vm == null) { setGraphic(null); return; }
                title.setText(vm.name);
                sub.setText(vm.count + (vm.count==1 ? " chat" : " chats"));
                trash.setVisible(vm.deletable);
                trash.setManaged(vm.deletable);
                trash.setOnAction(e -> getListView().getItems().remove(vm));
                setGraphic(root);
            }
        });

        foldersList.setOnMouseClicked(e -> {
            FolderVM sel = foldersList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            switch (sel.name.toLowerCase(Locale.ROOT)) {
                case "channels" -> activeFolder = ActiveFolder.CHANNELS;
                case "groups"   -> activeFolder = ActiveFolder.GROUPS;
                case "pv"       -> activeFolder = ActiveFolder.PV;
                default         -> activeFolder = ActiveFolder.ALL;
            }
            updateFilterPredicate();
            closeFoldersDrawer();
        });
    }

    private void refreshFoldersList() {
        if (foldersList == null) return;

        int all = allChats.size();
        int ch  = (int) allChats.stream().filter(c -> c.kind == ChatKind.CHANNEL).count();
        int gr  = (int) allChats.stream().filter(c -> c.kind == ChatKind.GROUP).count();
        int pv  = (int) allChats.stream().filter(c -> c.kind == ChatKind.DIRECT).count();

        ObservableList<FolderVM> data = FXCollections.observableArrayList(
                new FolderVM("All chats", all, false),
                new FolderVM("Channels", ch, false),
                new FolderVM("Groups",   gr, false),
                new FolderVM("PV",       pv, false)
        );
        foldersList.setItems(data);
    }

    private void openFoldersDrawer() {
        if (foldersOverlay == null || foldersOpen) return;
        foldersOpen = true;

        foldersOverlay.setVisible(true);
        foldersOverlay.setManaged(true);
        foldersOverlay.applyCss();
        foldersOverlay.layout();

        double w = foldersDrawer.getWidth() <= 0 ? foldersDrawer.prefWidth(-1) : foldersDrawer.getWidth();
        overlayScrim.setOpacity(0);
        foldersDrawer.setTranslateX(-w);

        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(120), overlayScrim);
        fade.setToValue(0.35);

        TranslateTransition slide = new TranslateTransition(javafx.util.Duration.millis(180), foldersDrawer);
        slide.setToX(0);

        fade.play();
        slide.play();
    }

    @FXML
    public void closeFoldersDrawer() {
        if (foldersOverlay == null || !foldersOpen) {
            if (foldersOverlay != null) { foldersOverlay.setVisible(false); foldersOverlay.setManaged(false); }
            return;
        }
        foldersOpen = false;

        double w = foldersDrawer.getWidth() <= 0 ? foldersDrawer.prefWidth(-1) : foldersDrawer.getWidth();

        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(120), overlayScrim);
        fade.setToValue(0.0);

        TranslateTransition slide = new TranslateTransition(javafx.util.Duration.millis(180), foldersDrawer);
        slide.setToX(-w);
        slide.setOnFinished(e -> {
            foldersOverlay.setVisible(false);
            foldersOverlay.setManaged(false);
        });

        fade.play();
        slide.play();
    }

    @FXML
    private void openCreateFolderDialog() {
        // Return type must be ButtonType so we can check OK/CANCEL
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("New Folder");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        TextField name = new TextField();
        name.setPromptText("Folder name");

        VBox box = new VBox(10,
                new Label("Folder name"),
                name,
                new Separator(),
                new Label("Included chats"),
                new Label("Add Chats  (+)   —  (UI demo)"),
                new Separator(),
                new Label("Excluded chats"),
                new Label("Add Chats to Exclude  (–)  —  (UI demo)")
        );
        box.setPadding(new Insets(8));
        dlg.getDialogPane().setContent(box);

        // Now the lambda param is ButtonType (not Void)
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String n = Optional.ofNullable(name.getText()).orElse("").trim();
                if (!n.isEmpty()) {
                    foldersList.getItems().add(new FolderVM(n, 0, true));
                }
            }
        });
    }

}



//package com.yamyam.messenger.client.gui.controller.chat;
//
//import javafx.animation.FadeTransition;
//import javafx.animation.TranslateTransition;
//
//import com.yamyam.messenger.client.gui.theme.ThemeManager;
//import com.yamyam.messenger.client.network.service.ContactService;
//import com.yamyam.messenger.client.network.dto.Contact;
//import com.yamyam.messenger.client.util.AppSession;
//import com.yamyam.messenger.client.util.ServiceLocator;
//import com.yamyam.messenger.shared.model.Users;
//import javafx.application.Platform;
//import javafx.beans.binding.Bindings;
//import javafx.collections.*;
//import javafx.collections.transformation.FilteredList;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.*;
//import javafx.scene.Scene;
//import javafx.scene.control.*;
//import javafx.scene.image.Image;
//import javafx.scene.image.ImageView;
//import javafx.scene.layout.*;
//import javafx.util.Callback;
//
//import java.net.URL;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//
//public class ChatController implements Initializable {
//
//    /* -----* *------
//       FXML references (wired from .fxml)
//       -----* *------ */
//    @FXML private TextField searchField;
//    @FXML private ListView<ChatItem> chatList;
//    @FXML private ListView<Msg> messageList;
//    @FXML private TextArea inputField;
//    @FXML private Button sendBtn;
//
//    @FXML private ImageView headerAvatar;
//    @FXML private Label headerName, headerStatus;
//
//    @FXML private VBox infoPane;
//    @FXML private ImageView infoAvatar;
//    @FXML private Label infoName, infoPresence;
//    @FXML private FlowPane mediaGrid;
//
//    @FXML private MenuButton themeBtn;
//    @FXML private RadioMenuItem miLight, miDark, miAmoled;
//
//    // Drawer / overlay (Folders)
//    @FXML private StackPane foldersOverlay;
//    @FXML private Pane overlayScrim;
//    @FXML private VBox foldersDrawer;
//    @FXML private ListView<FolderVM> foldersList;
//    @FXML private Button btnCreateFolder, btnHamburger;
//
//    /* -----* *------
//       Controller state & formatting
//       -----* *------ */
//    private final ObservableList<ChatItem> allChats = FXCollections.observableArrayList();
//
//    // unified filtered view (search + folder)
//    private final FilteredList<ChatItem> filteredChats =
//            new FilteredList<>(allChats, c -> true);
//
//    private final Image placeholder = new Image(
//            Objects.requireNonNull(getClass().getResource(
//                    "/com/yamyam/messenger/client/gui/images/profileDefault.jpg")).toExternalForm()
//    );
//
//    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
//
//    /* -----* *------
//       Lifecycle
//       - Initializes list renderers, message renderer, and composer (send area).
//       - Loads contacts from the current service and injects them via loadChats(...).
//       - Applies CSS and re-applies theme safely on FX thread.
//       -----* *------ */
//    @Override
//    public void initialize(URL url, ResourceBundle rb) {
//        if (!AppSession.isLoggedIn()) {
//            System.err.println("❌ No user in session — redirecting or blocking UI");
//            return;
//        }
//
//        Users me = AppSession.getCurrentUser();
//        System.out.println("✅ Logged in as: " + me.getEmail());
//
//        setupChatList();
//        setupMessageList();
//        setupComposer();
//
//        loadContactsFromService();
//
//        if (!allChats.isEmpty()) {
//            chatList.getSelectionModel().select(0);
//            openChat(chatList.getSelectionModel().getSelectedItem());
//        } else {
//            System.out.println("ℹ️ No chats available for user: " + me.getEmail());
//        }
//
//        Platform.runLater(() -> {
//            Scene scene = headerName.getScene();
//            if (scene == null) return;
//
//            var urlCss = getClass().getResource("/com/yamyam/messenger/client/gui/styles/chat.css");
//            if (urlCss != null) {
//                String chatCss = urlCss.toExternalForm();
//                if (!scene.getStylesheets().contains(chatCss)) {
//                    scene.getStylesheets().add(chatCss);
//                }
//            }
//
//            var root = scene.getRoot();
//            var classes = root.getStyleClass();
//            if (!classes.contains("themed")) classes.add("themed");
//            if (!classes.contains("chat-root")) classes.add("chat-root");
//
//            ThemeManager.reapply(scene);
//            syncThemeMenu(ThemeManager.current());
//
//            // NEW: wire unified filtering + folders drawer
//            hookCombinedFiltering();   // search + folder (folder from drawer clicks)
//            setupFoldersDrawer();      // cell factory for drawer list
//            refreshFoldersList();      // initial counts (All/Channels/Groups/PV)
//            closeFoldersDrawer();      // ensure closed initially
//        });
//    }
//
//    /* ================== Chat list ================== */
//
//    /* -----* *------
//       ListView<ChatItem> cell factory
//       - Renders avatar, title, and last message preview.
//       - Shows "• Online" only if DIRECT and the contact is online.
//       - Shows a tiny type tag for GROUP/CHANNEL (no icon dependency).
//       -----* *------ */
//    private void setupChatList() {
//        chatList.setCellFactory(new Callback<>() {
//            @Override public ListCell<ChatItem> call(ListView<ChatItem> lv) {
//                return new ListCell<>() {
//                    private final ImageView avatar = new ImageView();
//                    private final Label name = new Label();
//                    private final Label last = new Label();
//                    private final VBox labels = new VBox(name, last);
//                    private final HBox root = new HBox(10, avatar, labels);
//                    {
//                        root.setAlignment(Pos.CENTER_LEFT);
//                        root.setPadding(new Insets(10,12,10,12));
//                        avatar.setFitWidth(40);
//                        avatar.setFitHeight(40);
//                        avatar.setPreserveRatio(true);
//                        name.getStyleClass().add("chat-conv__name");
//                        last.getStyleClass().add("chat-conv__status");
//                    }
//                    @Override protected void updateItem(ChatItem it, boolean empty) {
//                        super.updateItem(it, empty);
//                        if (empty || it==null) { setGraphic(null); return; }
//                        avatar.setImage(it.avatar != null ? it.avatar : placeholder);
//
//                        String typeTag = switch (it.kind) {
//                            case DIRECT -> "";
//                            case GROUP -> "  • Group";
//                            case CHANNEL -> "  • Channel";
//                        };
//                        String presence = (it.kind == ChatKind.DIRECT && it.online) ? " • Online" : "";
//                        name.setText(it.title + typeTag + presence);
//
//                        last.setText(it.lastMessagePreview());
//                        setGraphic(root);
//                    }
//                };
//            }
//        });
//
//        // IMPORTANT: chatList items are provided by FilteredList (search+folder).
//        chatList.setItems(filteredChats);
//
//        /* -----* *------
//           Selection listener:
//           - Opens the selected chat in the middle pane.
//           - Notifies backend that the chat is opened (for presence/seen/typing).
//           -----* *------ */
//        chatList.getSelectionModel().selectedItemProperty().addListener((o, oldSel, sel) -> {
//            if (sel == null) return;
//            openChat(sel);
//
//            long meUserId = AppSession.isLoggedIn() ? AppSession.requireUserId() : 1L; // TEMP until login is wired
//            try {
//                ServiceLocator.chat().openChat(meUserId, sel.contactId);
//            } catch (Exception ex) {
//                System.err.println("[chat] openChat failed: " + ex.getMessage());
//            }
//        });
//    }
//
//    /* ================== Messages ================== */
//
//    /* -----* *------
//       Message list renderer
//       - Simple bubble renderer; aligns right for "me" and left for "other".
//       - Wrap text and limit bubble width responsively.
//       - Auto-scroll to bottom on new items (listener added below).
//       -----* *------ */
//    private void setupMessageList() {
//        messageList.setCellFactory(lv -> new ListCell<>() {
//            private final Label bubble = new Label();
//            private final HBox line = new HBox(bubble);
//            {
//                bubble.getStyleClass().add("bubble");
//                bubble.setWrapText(true);
//                bubble.maxWidthProperty().bind(messageList.widthProperty().subtract(120));
//                setPadding(new Insets(6,12,6,12));
//            }
//            @Override protected void updateItem(Msg m, boolean empty) {
//                super.updateItem(m, empty);
//                if (empty || m == null) { setGraphic(null); return; }
//                bubble.setText(m.text + "  " + timeFmt.format(m.at));
//                bubble.getStyleClass().removeAll("me", "other");
//                bubble.getStyleClass().add(m.isMe ? "me" : "other");
//                line.setAlignment(m.isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
//                line.setNodeOrientation(m.isMe ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
//                setGraphic(line);
//            }
//        });
//
//        // Auto scroll to latest message
//        messageList.getItems().addListener((ListChangeListener<? super Msg>) c ->
//                Platform.runLater(() -> messageList.scrollTo(messageList.getItems().size() - 1)));
//    }
//
//    /* ================== Composer (input + send) ================== */
//
//    /* -----* *------
//       Composer setup
//       - Disables send if input is blank.
//       - ENTER sends; SHIFT+ENTER inserts newline.
//       -----* *------ */
//    private void setupComposer() {
//        sendBtn.disableProperty().bind(
//                Bindings.createBooleanBinding(
//                        () -> inputField.getText() == null || inputField.getText().trim().isEmpty(),
//                        inputField.textProperty())
//        );
//        inputField.setOnKeyPressed(e -> {
//            switch (e.getCode()) {
//                case ENTER -> { if (!e.isShiftDown()) { e.consume(); sendMessage(); } }
//            }
//        });
//    }
//
//    /* -----* *------
//       sendMessage()
//       - Adds an optimistic "me" message to the current chat.
//       - Clears the input field.
//       - Simulates a tiny auto-reply to keep the UI alive (until real backend is wired).
//       -----* *------ */
//    @FXML
//    private void sendMessage() {
//        // Text is read from the input field
//        String text = Optional.ofNullable(inputField.getText()).orElse("").trim();
//        if (text.isEmpty()) {
//            return;
//        }
//
//        // The currently active chat is selected
//        ChatItem selectedChat = chatList.getSelectionModel().getSelectedItem();
//        if (selectedChat == null) {
//            return;
//        }
//
//        // The sending command is delegated to the service layer
////        try {
////            ServiceLocator.chat().sendMessage(selectedChat.contactId, text);
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
//
//        inputField.clear();
//    }
//
//    /* ================== Theme ================== */
//
//    /* -----* *------
//       Theme switching (wired from FXML)
//       -----* *------ */
//    @FXML public void setThemeLight()  { switchTo(ThemeManager.Theme.LIGHT); }
//    @FXML public void setThemeDark()   { switchTo(ThemeManager.Theme.DARK); }
//    @FXML public void setThemeAmoled() { switchTo(ThemeManager.Theme.AMOLED); }
//
//    @FXML public void cycleTheme() {
//        var cur = ThemeManager.current();
//        var next = switch (cur) {
//            case LIGHT  -> ThemeManager.Theme.DARK;
//            case DARK   -> ThemeManager.Theme.AMOLED;
//            case AMOLED -> ThemeManager.Theme.LIGHT;
//        };
//        switchTo(next);
//    }
//
//    private void switchTo(ThemeManager.Theme t) {
//        var scene = headerName.getScene();
//        if (scene == null) return;
//        ThemeManager.apply(scene, t);
//        ThemeManager.reapply(scene);
//        syncThemeMenu(t);
//    }
//
//    private void syncThemeMenu(ThemeManager.Theme t) {
//        if (miLight  != null) miLight.setSelected(t == ThemeManager.Theme.LIGHT);
//        if (miDark   != null) miDark.setSelected(t == ThemeManager.Theme.DARK);
//        if (miAmoled != null) miAmoled.setSelected(t == ThemeManager.Theme.AMOLED);
//        if (themeBtn != null) themeBtn.setText("Theme");
//    }
//
//    /* ================== Public loading APIs (Stage-1) ================== */
//
//    /* -----* *------
//       loadChats(List<Contact> contacts)
//       - PUBLIC entry point for current app flow (DIRECT chats via existing Contact DTO).
//       - Converts Contact -> ChatRef(DIRECT) internally and delegates to loadChatsGeneric(...).
//       - Backend/DataManager should call this after fetching user's chats by userId.
//       -----* *------ */
//    public void loadChats(List<Contact> contacts) {
//        if (contacts == null || contacts.isEmpty()) {
//            loadChatsGeneric(Collections.emptyList());
//            return;
//        }
//
//        List<ChatRef> refs = new ArrayList<>(contacts.size());
//        for (Contact c : contacts) {
//            ChatKind kind = switch (c.type()) {
//                case DIRECT -> ChatKind.DIRECT;
//                case GROUP -> ChatKind.GROUP;
//                case CHANNEL -> ChatKind.CHANNEL;
//            };
//
//            refs.add(new ChatRef(
//                    c.id(),
//                    kind,
//                    c.title(),
//                    c.avatarUrl(),
//                    c.online(),
//                    c.memberCount(),
//                    false,
//                    0
//            ));
//        }
//
//        loadChatsGeneric(refs);
//    }
//
//    /* -----* *------
//       loadChatsGeneric(List<ChatRef> refs)
//       - PUBLIC, future-proof entry point for DIRECT/GROUP/CHANNEL.
//       - UI-only: builds ChatItem list, preserves previous selection, opens selected chat.
//       - SAFE: if list is empty, clears middle pane headers and images gracefully.
//       -----* *------ */
//    public void loadChatsGeneric(List<ChatRef> refs) {
//        Long keepSelectedId = Optional.ofNullable(chatList.getSelectionModel().getSelectedItem())
//                .map(ci -> ci.contactId).orElse(null);
//
//        var safe = (refs == null) ? List.<ChatRef>of() : refs;
//        var newItems = new ArrayList<ChatItem>(safe.size());
//        for (ChatRef r : safe) {
//            Image av = (r.avatarUrl != null && !r.avatarUrl.isBlank())
//                    ? new Image(r.avatarUrl(), true) : null;
//            newItems.add(ChatItem.fromRef(r, av));
//        }
//
//        allChats.setAll(newItems);
//
//        // IMPORTANT: keep ListView bound to filtered list (search + folder)
//        chatList.setItems(filteredChats);
//
//        if (!allChats.isEmpty()) {
//            int idx = 0;
//            if (keepSelectedId != null) {
//                for (int i = 0; i < allChats.size(); i++) {
//                    if (allChats.get(i).contactId == keepSelectedId) { idx = i; break; }
//                }
//            }
//            chatList.getSelectionModel().select(idx);
//            openChat(chatList.getSelectionModel().getSelectedItem());
//        } else {
//            messageList.setItems(FXCollections.observableArrayList());
//            headerName.setText("");
//            headerStatus.setText("");
//            infoName.setText("");
//            infoPresence.setText("");
//            headerAvatar.setImage(placeholder);
//            infoAvatar.setImage(placeholder);
//        }
//
//        // keep folders drawer counts & filter in sync
//        refreshFoldersList();
//        updateFilterPredicate();
//    }
//
//    /* ================== Helpers ================== */
//
//    /* -----* *------
//       openChat(ChatItem c)
//       - Updates header/avatar/presence info based on the selected chat type.
//       - Binds messageList items to the selected ChatItem.messages observable.
//       -----* *------ */
//    private void openChat(ChatItem c) {
//        headerAvatar.setImage(c.avatar != null ? c.avatar : placeholder);
//        headerName.setText(c.title);
//
//        String status = switch (c.kind) {
//            case DIRECT  -> (c.online ? "Online" : "Last seen recently");
//            case GROUP   -> (c.memberCount != null ? c.memberCount + " members" : "Group");
//            case CHANNEL -> "Channel";
//        };
//        headerStatus.setText(status);
//
//        infoAvatar.setImage(c.avatar != null ? c.avatar : placeholder);
//        infoName.setText(c.title);
//        infoPresence.setText(status);
//
//        messageList.setItems(c.messages);
//    }
//
//    /* -----* *------
//       Current app flow helper:
//       - Reads contacts via ServiceLocator and injects them using the public API.
//       - Keeps behavior identical to previous implementation.
//       -----* *------ */
//
//    // Fetch contacts via the real ContactService and fill the chat list
//    private void loadContactsFromService() {
//        ContactService contactService = ServiceLocator.contacts();
//
//        new Thread(() -> {
//            try {
//                long meUserId = AppSession.requireUserId();
//
//                final List<Contact> contacts = contactService.getContacts(meUserId);
//
//                Platform.runLater(() -> {
//                    allChats.clear();
//                    for (Contact c : contacts) {
//                        Image av = (c.avatarUrl() != null && !c.avatarUrl().isBlank())
//                                ? new Image(c.avatarUrl(), true) : null;
//                        allChats.add(ChatItem.fromContact(c, av));
//                    }
//                    // keep UI in sync
//                    refreshFoldersList();
//                    updateFilterPredicate();
//                });
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                Platform.runLater(() -> {
//                    new Alert(Alert.AlertType.ERROR, "Failed to load contacts.").showAndWait();
//                });
//            }
//        }).start();
//    }
//
//    private static LocalDateTime now(int m){ return LocalDateTime.now().minusMinutes(m); }
//
//    /* ================== Mini models (UI-only) ================== */
//
//    /* -----* *------
//       ChatKind
//       - DIRECT: one-to-one chat (presence applies)
//       - GROUP : multi-member chat (optionally show memberCount)
//       - CHANNEL: broadcast style (no presence)
//       -----* *------ */
//    public enum ChatKind { DIRECT, GROUP, CHANNEL }
//
//    /* -----* *------
//       ChatRef
//       - Generic, data-source-agnostic input model for loading chats into the UI.
//       - Use this for mixed lists (DIRECT/GROUP/CHANNEL) once the backend supports them.
//       -----* *------ */
//    public static final class ChatRef {
//        public final long id;
//        public final ChatKind kind;
//        public final String title;
//        public final String avatarUrl;
//        public final boolean online;         // only meaningful for DIRECT
//        public final Integer memberCount;    // useful for GROUP
//        public final boolean muted;          // reserved for notifications
//        public final int unreadCount;        // reserved badge
//
//        public ChatRef(long id, ChatKind kind, String title, String avatarUrl,
//                       boolean online, Integer memberCount, boolean muted, int unreadCount) {
//            this.id = id; this.kind = kind; this.title = title; this.avatarUrl = avatarUrl;
//            this.online = online; this.memberCount = memberCount; this.muted = muted; this.unreadCount = unreadCount;
//        }
//    }
//
//    /* -----* *------
//       ChatItem (UI view-model)
//       - Holds just enough data to render the left list and the middle pane header.
//       - messages is an ObservableList bound to the messageList view.
//       - 'online' is mutable so presence updates can be reflected later (WS).
//       -----* *------ */
//    public static final class ChatItem {
//        public final long contactId;         // ID of the target (DIRECT/GROUP/CHANNEL)
//        public final String title;
//        public boolean online;               // presence (applies for DIRECT)
//        public final Image avatar;
//        public final ChatKind kind;
//        public final Integer memberCount;    // for GROUP
//        public final boolean muted;          // reserved
//        public final ObservableList<Msg> messages = FXCollections.observableArrayList();
//
//        private ChatItem(long id, String t, boolean o, Image a, ChatKind k, Integer memberCount, boolean muted) {
//            contactId=id; title=t; online=o; avatar=a; kind=k; this.memberCount = memberCount; this.muted = muted;
//        }
//
//        public static ChatItem fromContact(Contact c, Image avatar) {
//            return new ChatItem(c.id(), c.title(), c.online(), avatar, ChatKind.DIRECT, null, false);
//        }
//
//        public static ChatItem fromRef(ChatRef r, Image avatar) {
//            boolean online = (r.kind == ChatKind.DIRECT) && r.online;
//            return new ChatItem(r.id, r.title, online, avatar, r.kind, r.memberCount, r.muted);
//        }
//
//        public static ChatItem of(String t, boolean o, Image a, List<Msg> msgs){
//            var c = new ChatItem(-1, t, o, a, ChatKind.DIRECT, null, false);
//            c.messages.addAll(msgs); return c;
//        }
//
//        public String lastMessagePreview(){
//            if (messages.isEmpty()) return "";
//            String t = messages.get(messages.size()-1).text;
//            return t.length()>30 ? t.substring(0,30)+"…" : t;
//        }
//    }
//
//    /* -----* *------
//       Msg (UI model for a chat bubble)
//       - Minimal for Stage-1: who sent it (me/other), text, and timestamp.
//       - Delivery/seen states can be added later without breaking the current UI.
//       -----* *------ */
//    public static final class Msg {
//        public final boolean isMe;
//        public final String text;
//        public final LocalDateTime at;
//
//        public Msg(boolean m, String t, LocalDateTime a){ isMe=m; text=t; at=a; }
//    }
//
//    // Folder view-model for the drawer list
//    public static final class FolderVM {
//        public final String name;
//        public final int count;
//        public final boolean deletable;
//        public FolderVM(String n, int c, boolean d){ name=n, count=c, deletable=d; }
//    }
//
//    // Active folder filter
//    private enum ActiveFolder { ALL, CHANNELS, GROUPS, PV }
//    private ActiveFolder activeFolder = ActiveFolder.ALL;
//
//    /* ================== Filtering (search + folder) ================== */
//
//    private void hookCombinedFiltering() {
//        // ListView shows the filtered view
//        chatList.setItems(filteredChats);
//
//        // react to search text
//        searchField.textProperty().addListener((o, ov, nv) -> updateFilterPredicate());
//
//        // apply once at start
//        updateFilterPredicate();
//    }
//
//    private void updateFilterPredicate() {
//        final String q = Optional.ofNullable(searchField.getText())
//                .orElse("").trim().toLowerCase(Locale.ROOT);
//
//        filteredChats.setPredicate(ci -> {
//            boolean folderOk = switch (activeFolder) {
//                case ALL      -> true;
//                case CHANNELS -> ci.kind == ChatKind.CHANNEL;
//                case GROUPS   -> ci.kind == ChatKind.GROUP;
//                case PV       -> ci.kind == ChatKind.DIRECT;
//            };
//            boolean searchOk = q.isEmpty()
//                    || ci.title.toLowerCase(Locale.ROOT).contains(q)
//                    || ci.lastMessagePreview().toLowerCase(Locale.ROOT).contains(q);
//            return folderOk && searchOk;
//        });
//    }
//
//    /* ================== Folders Drawer ================== */
//
//    private boolean foldersOpen = false;
//
//    @FXML public void openFoldersManager() { openFoldersDrawer(); }
//
//    private void setupFoldersDrawer() {
//        if (foldersList == null) return;
//
//        // Pretty folder rows (icon + title + count + optional trash)
//        foldersList.setCellFactory(lv -> new ListCell<>() {
//            private final Label icon  = new Label("📁");
//            private final Label title = new Label();
//            private final Label sub   = new Label();
//            private final Button trash = new Button("🗑");
//            private final VBox texts = new VBox(title, sub);
//            private final Pane spacer = new Pane();
//            private final HBox root = new HBox(10, icon, texts, spacer, trash);
//            {
//                root.setAlignment(Pos.CENTER_LEFT);
//                HBox.setHgrow(spacer, Priority.ALWAYS);
//                root.getStyleClass().add("folder-row");
//                title.getStyleClass().add("folder-row__title");
//                sub.getStyleClass().add("folder-row__sub");
//                trash.getStyleClass().add("folder-row__trash");
//            }
//            @Override protected void updateItem(FolderVM vm, boolean empty) {
//                super.updateItem(vm, empty);
//                if (empty || vm == null) { setGraphic(null); return; }
//                title.setText(vm.name);
//                sub.setText(vm.count + (vm.count==1 ? " chat" : " chats"));
//                trash.setVisible(vm.deletable);
//                trash.setManaged(vm.deletable);
//                trash.setOnAction(e -> getListView().getItems().remove(vm));
//                setGraphic(root);
//            }
//        });
//
//        // click selection -> apply filter and close
//        foldersList.setOnMouseClicked(e -> {
//            FolderVM sel = foldersList.getSelectionModel().getSelectedItem();
//            if (sel == null) return;
//            switch (sel.name.toLowerCase(Locale.ROOT)) {
//                case "channels" -> activeFolder = ActiveFolder.CHANNELS;
//                case "groups"   -> activeFolder = ActiveFolder.GROUPS;
//                case "pv"       -> activeFolder = ActiveFolder.PV;
//                default         -> activeFolder = ActiveFolder.ALL;
//            }
//            updateFilterPredicate();
//            closeFoldersDrawer();
//        });
//    }
//
//    private void refreshFoldersList() {
//        if (foldersList == null) return;
//
//        int all = allChats.size();
//        int ch  = (int) allChats.stream().filter(c -> c.kind == ChatKind.CHANNEL).count();
//        int gr  = (int) allChats.stream().filter(c -> c.kind == ChatKind.GROUP).count();
//        int pv  = (int) allChats.stream().filter(c -> c.kind == ChatKind.DIRECT).count();
//
//        ObservableList<FolderVM> data = FXCollections.observableArrayList(
//                new FolderVM("All chats", all, false),
//                new FolderVM("Channels", ch, false),
//                new FolderVM("Groups",   gr, false),
//                new FolderVM("PV",       pv, false)
//                // user folders can be appended here later (deletable = true)
//        );
//        foldersList.setItems(data);
//    }
//
//    private void openFoldersDrawer() {
//        if (foldersOverlay == null || foldersOpen) return;
//        foldersOpen = true;
//
//        // ensure layout to get correct width
//        foldersOverlay.setVisible(true);
//        foldersOverlay.setManaged(true);
//        foldersOverlay.applyCss();
//        foldersOverlay.layout();
//
//        double w = foldersDrawer.getWidth() <= 0 ? foldersDrawer.prefWidth(-1) : foldersDrawer.getWidth();
//        overlayScrim.setOpacity(0);
//        foldersDrawer.setTranslateX(-w);
//
//        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(120), overlayScrim);
//        fade.setToValue(0.35);
//
//        TranslateTransition slide = new TranslateTransition(javafx.util.Duration.millis(180), foldersDrawer);
//        slide.setToX(0);
//
//        fade.play();
//        slide.play();
//    }
//
//    @FXML
//    public void closeFoldersDrawer() {
//        if (foldersOverlay == null || !foldersOpen) {
//            if (foldersOverlay != null) { foldersOverlay.setVisible(false); foldersOverlay.setManaged(false); }
//            return;
//        }
//        foldersOpen = false;
//
//        double w = foldersDrawer.getWidth() <= 0 ? foldersDrawer.prefWidth(-1) : foldersDrawer.getWidth();
//
//        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(120), overlayScrim);
//        fade.setToValue(0.0);
//
//        TranslateTransition slide = new TranslateTransition(javafx.util.Duration.millis(180), foldersDrawer);
//        slide.setToX(-w);
//        slide.setOnFinished(e -> {
//            foldersOverlay.setVisible(false);
//            foldersOverlay.setManaged(false);
//        });
//
//        fade.play();
//        slide.play();
//    }
//
//    @FXML
//    private void openCreateFolderDialog() {
//        Dialog<Void> dlg = new Dialog<>();
//        dlg.setTitle("New Folder");
//        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
//
//        TextField name = new TextField();
//        name.setPromptText("Folder name");
//
//        VBox box = new VBox(10,
//                new Label("Folder name"),
//                name,
//                new Separator(),
//                new Label("Included chats"),
//                new Label("Add Chats  (+)   —  (UI demo)"),
//                new Separator(),
//                new Label("Excluded chats"),
//                new Label("Add Chats to Exclude  (–)  —  (UI demo)")
//        );
//        box.setPadding(new Insets(8));
//        dlg.getDialogPane().setContent(box);
//
//        dlg.showAndWait().ifPresent(btn -> {
//            if (btn == ButtonType.OK) {
//                String n = Optional.ofNullable(name.getText()).orElse("").trim();
//                if (!n.isEmpty()) {
//                    // For now, just append a custom, deletable folder with 0 items.
//                    foldersList.getItems().add(new FolderVM(n, 0, true));
//                }
//            }
//        });
//    }
//}