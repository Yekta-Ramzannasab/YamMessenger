package com.yamyam.messenger.client.gui.controller.chat;

import com.yamyam.messenger.client.gui.theme.ThemeManager;
import com.yamyam.messenger.client.network.service.ContactService;
import com.yamyam.messenger.client.network.dto.Contact;
import com.yamyam.messenger.client.util.AppSession;
import com.yamyam.messenger.client.util.ServiceLocator;
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

    /* -----* *------
       Controller state & formatting
       -----* *------ */
    private final ObservableList<ChatItem> allChats = FXCollections.observableArrayList();

    private final Image placeholder = new Image(
            Objects.requireNonNull(getClass().getResource(
                    "/com/yamyam/messenger/client/gui/images/profileDefault.jpg")).toExternalForm()
    );

    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    /* -----* *------
       Lifecycle
       - Initializes list renderers, message renderer, and composer (send area).
       - Loads contacts from the current service and injects them via loadChats(...).
       - Applies CSS and re-applies theme safely on FX thread.
       -----* *------ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupChatList();
        setupMessageList();
        setupComposer();

        // Stage-1: keep current behavior (load from service) and then feed the UI via public API.
        loadContactsFromService();

        // Select the first chat (if any) to avoid an empty middle pane.
        if (!allChats.isEmpty()) {
            chatList.getSelectionModel().select(0);
            openChat(chatList.getSelectionModel().getSelectedItem());
        }

        // Ensure CSS & root style classes, then re-apply current theme
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
    }

    /* ================== Chat list ================== */

    /* -----* *------
       ListView<ChatItem> cell factory
       - Renders avatar, title, and last message preview.
       - Shows "• Online" only if DIRECT and the contact is online.
       - Shows a tiny type tag for GROUP/CHANNEL (no icon dependency).
       -----* *------ */
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

        /* -----* *------
           Search filter (by title or last message preview)
           - Keeps allChats as the master source and applies a filtered view.
           -----* *------ */
        searchField.textProperty().addListener((o, ov, nv) -> {
            String q = nv == null ? "" : nv.trim().toLowerCase(Locale.ROOT);
            chatList.setItems(q.isEmpty() ? allChats :
                    allChats.filtered(c ->
                            c.title.toLowerCase(Locale.ROOT).contains(q) ||
                                    c.lastMessagePreview().toLowerCase(Locale.ROOT).contains(q)));
        });

        /* -----* *------
           Selection listener:
           - Opens the selected chat in the middle pane.
           - Notifies backend that the chat is opened (for presence/seen/typing).
           -----* *------ */
        chatList.getSelectionModel().selectedItemProperty().addListener((o, oldSel, sel) -> {
            if (sel == null) return;
            openChat(sel);

            long meUserId = AppSession.isLoggedIn() ? AppSession.requireUserId() : 1L; // TEMP until login is wired
            try {
                ServiceLocator.chat().openChat(meUserId, sel.contactId);
            } catch (Exception ex) {
                System.err.println("[chat] openChat failed: " + ex.getMessage());
            }
        });
    }

    /* ================== Messages ================== */

    /* -----* *------
       Message list renderer
       - Simple bubble renderer; aligns right for "me" and left for "other".
       - Wrap text and limit bubble width responsively.
       - Auto-scroll to bottom on new items (listener added below).
       -----* *------ */
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

        // Auto scroll to latest message
        messageList.getItems().addListener((ListChangeListener<? super Msg>) c ->
                Platform.runLater(() -> messageList.scrollTo(messageList.getItems().size() - 1)));
    }

    /* ================== Composer (input + send) ================== */

    /* -----* *------
       Composer setup
       - Disables send if input is blank.
       - ENTER sends; SHIFT+ENTER inserts newline.
       -----* *------ */
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

    /* -----* *------
       sendMessage()
       - Adds an optimistic "me" message to the current chat.
       - Clears the input field.
       - Simulates a tiny auto-reply to keep the UI alive (until real backend is wired).
       -----* *------ */
    @FXML
    private void sendMessage() {
        // Text is read from the input field
        String text = Optional.ofNullable(inputField.getText()).orElse("").trim();
        if (text.isEmpty()) {
            return;
        }

        // The currently active chat is selected
        ChatItem selectedChat = chatList.getSelectionModel().getSelectedItem();
        if (selectedChat == null) {
            return;
        }

        // The sending command is delegated to the service layer
        try {
            ServiceLocator.chat().sendMessage(selectedChat.contactId, text);
        } catch (Exception e) {
            e.printStackTrace();
        }

        inputField.clear();
    }

    /* ================== Theme ================== */

    /* -----* *------
       Theme switching (wired from FXML)
       -----* *------ */
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

    /* ================== Public loading APIs (Stage-1) ================== */

    /* -----* *------
       loadChats(List<Contact> contacts)
       - PUBLIC entry point for current app flow (DIRECT chats via existing Contact DTO).
       - Converts Contact -> ChatRef(DIRECT) internally and delegates to loadChatsGeneric(...).
       - Backend/DataManager should call this after fetching user's chats by userId.
       -----* *------ */
    public void loadChats(List<Contact> contacts) {
        if (contacts == null) {
            loadChatsGeneric(Collections.emptyList());
            return;
        }
        List<ChatRef> refs = new ArrayList<>(contacts.size());
        for (Contact c : contacts) {
            refs.add(new ChatRef(
                    c.id(),
                    ChatKind.DIRECT,
                    c.title(),
                    c.avatarUrl(),
                    c.online(),
                    null,      // memberCount (not used for DIRECT)
                    false,     // muted (reserved)
                    0          // unreadCount (reserved)
            ));
        }
        loadChatsGeneric(refs);
    }

    /* -----* *------
       loadChatsGeneric(List<ChatRef> refs)
       - PUBLIC, future-proof entry point for DIRECT/GROUP/CHANNEL.
       - UI-only: builds ChatItem list, preserves previous selection, opens selected chat.
       - SAFE: if list is empty, clears middle pane headers and images gracefully.
       -----* *------ */
    public void loadChatsGeneric(List<ChatRef> refs) {
        Long keepSelectedId = Optional.ofNullable(chatList.getSelectionModel().getSelectedItem())
                .map(ci -> ci.contactId).orElse(null);

        var safe = (refs == null) ? List.<ChatRef>of() : refs;
        var newItems = new ArrayList<ChatItem>(safe.size());
        for (ChatRef r : safe) {
            Image av = (r.avatarUrl != null && !r.avatarUrl.isBlank())
                    ? new Image(r.avatarUrl, true) : null;
            newItems.add(ChatItem.fromRef(r, av));
        }

        allChats.setAll(newItems);
        chatList.setItems(allChats);

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
    }

    /* ================== Helpers ================== */

    /* -----* *------
       openChat(ChatItem c)
       - Updates header/avatar/presence info based on the selected chat type.
       - Binds messageList items to the selected ChatItem.messages observable.
       -----* *------ */
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

    /* -----* *------
       Current app flow helper:
       - Reads contacts via ServiceLocator and injects them using the public API.
       - Keeps behavior identical to previous implementation.
       -----* *------ */

    // Fetch contacts via the real ContactService and fill the chat list
    private void loadContactsFromService() {
        // get the call service from ServiceLocator. Now this is our actual adapter
        ContactService contactService = ServiceLocator.contacts();

        // All network work should be done in a background thread so that the UI doesn't lock up
        new Thread(() -> {
            try {
                // We read the logged-in user ID from the Session
                // We need to make sure that this ID is stored in the AppSession after login
                long meUserId = AppSession.requireUserId();

                // The UI requests the contact list from the service layer
                final List<Contact> contacts = contactService.getContacts(meUserId);

                // After receiving the response from the server, we need to update the UI in the main JavaFX thread
                Platform.runLater(() -> {
                    allChats.clear();
                    for (Contact c : contacts) {
                        Image av = (c.avatarUrl() != null && !c.avatarUrl().isBlank())
                                ? new Image(c.avatarUrl(), true) : null;
                        allChats.add(ChatItem.fromContact(c, av));
                    }
                    // The ListView is updated automatically because it is connected to allChats
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.ERROR, "Failed to load contacts.").showAndWait();
                });
            }
        }).start();
    }

    private static LocalDateTime now(int m){ return LocalDateTime.now().minusMinutes(m); }

    /* ================== Mini models (UI-only) ================== */

    /* -----* *------
       ChatKind
       - DIRECT: one-to-one chat (presence applies)
       - GROUP : multi-member chat (optionally show memberCount)
       - CHANNEL: broadcast style (no presence)
       -----* *------ */
    public enum ChatKind { DIRECT, GROUP, CHANNEL }

    /* -----* *------
       ChatRef
       - Generic, data-source-agnostic input model for loading chats into the UI.
       - Use this for mixed lists (DIRECT/GROUP/CHANNEL) once the backend supports them.
       -----* *------ */
    public static final class ChatRef {
        public final long id;
        public final ChatKind kind;
        public final String title;
        public final String avatarUrl;
        public final boolean online;         // only meaningful for DIRECT
        public final Integer memberCount;    // useful for GROUP
        public final boolean muted;          // reserved for notifications
        public final int unreadCount;        // reserved badge

        public ChatRef(long id, ChatKind kind, String title, String avatarUrl,
                       boolean online, Integer memberCount, boolean muted, int unreadCount) {
            this.id = id; this.kind = kind; this.title = title; this.avatarUrl = avatarUrl;
            this.online = online; this.memberCount = memberCount; this.muted = muted; this.unreadCount = unreadCount;
        }
    }

    /* -----* *------
       ChatItem (UI view-model)
       - Holds just enough data to render the left list and the middle pane header.
       - messages is an ObservableList bound to the messageList view.
       - 'online' is mutable so presence updates can be reflected later (WS).
       -----* *------ */
    public static final class ChatItem {
        public final long contactId;         // ID of the target (DIRECT/GROUP/CHANNEL)
        public final String title;
        public boolean online;               // presence (applies for DIRECT)
        public final Image avatar;
        public final ChatKind kind;
        public final Integer memberCount;    // for GROUP
        public final boolean muted;          // reserved
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

    /* -----* *------
       Msg (UI model for a chat bubble)
       - Minimal for Stage-1: who sent it (me/other), text, and timestamp.
       - Delivery/seen states can be added later without breaking the current UI.
       -----* *------ */
    public static final class Msg {
        public final boolean isMe;
        public final String text;
        public final LocalDateTime at;

        public Msg(boolean m, String t, LocalDateTime a){ isMe=m; text=t; at=a; }
    }
}
