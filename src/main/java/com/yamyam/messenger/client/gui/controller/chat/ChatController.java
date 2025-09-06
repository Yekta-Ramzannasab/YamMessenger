package com.yamyam.messenger.client.gui.controller.chat;

import com.yamyam.messenger.client.gui.theme.ThemeManager;
import com.yamyam.messenger.client.network.NetworkService;
import com.yamyam.messenger.client.network.dto.ContactType;
import com.yamyam.messenger.client.network.dto.SearchItem;
import com.yamyam.messenger.client.network.dto.SearchKind;
import com.yamyam.messenger.client.network.impl.NetworkChatServiceAdapter;
import com.yamyam.messenger.client.network.impl.SearchServiceAdapter;
import com.yamyam.messenger.client.network.service.ChatService;
import com.yamyam.messenger.client.network.service.ContactService;
import com.yamyam.messenger.client.network.dto.Contact;
import com.yamyam.messenger.client.network.service.SearchService;
import com.yamyam.messenger.client.util.AppSession;
import com.yamyam.messenger.client.util.ServiceLocator;
import com.yamyam.messenger.server.database.DataManager;
import com.yamyam.messenger.server.database.Database;
import com.yamyam.messenger.server.database.Search;
import com.yamyam.messenger.server.database.SearchResult;
import com.yamyam.messenger.shared.model.chat.Channel;
import com.yamyam.messenger.shared.model.chat.Chat;
import com.yamyam.messenger.shared.model.chat.GroupChat;
import com.yamyam.messenger.shared.model.chat.PrivateChat;
import com.yamyam.messenger.shared.model.message.MessageEntity;
import com.yamyam.messenger.shared.model.user.UserProfile;
import com.yamyam.messenger.shared.model.user.Users;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class ChatController implements Initializable {

    /* -----* *------
       FXML references (wired from .fxml)
       -----* *------ */
    @FXML private TextField searchField;
    @FXML private ListView<SearchItem> searchResults;
    private ListView<ChatItem> chatList = new ListView<>();
    @FXML private ListView<Msg> messageList;
    @FXML private TextArea inputField;
    @FXML private Button sendBtn;

    @FXML private Label infoBio;
    @FXML private Label infoUsername;
    @FXML private Label infoEmail;


    @FXML private ImageView headerAvatar;
    @FXML private Label headerName, headerStatus;

    @FXML private VBox infoPane;
    @FXML private ImageView infoAvatar;
    @FXML private Label infoName, infoPresence;
    @FXML private FlowPane mediaGrid;

    @FXML private MenuButton themeBtn;
    @FXML private RadioMenuItem miLight, miDark, miAmoled;
    @FXML private StackPane menuOverlay;
    @FXML private StackPane subPageOverlay;
    @FXML private VBox subPageContent;
    @FXML private Label subPageTitle;
    @FXML private Rectangle scrim;




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
        if (!AppSession.isLoggedIn()) {
            System.err.println("❌ No user in session — redirecting or blocking UI");
            return;
        }

        Users me = AppSession.getCurrentUser();
        System.out.println("✅ Logged in as: " + me.getEmail());


        setupSearchAndChatList();
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
                if (!scene.getStylesheets().contains(chatCss)) {
                    scene.getStylesheets().add(chatCss);
                }
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
    @SuppressWarnings("unchecked")
    private Users toUsers(Object e) {
        if (e instanceof Users u) return u;
        if (e instanceof java.util.Map<?, ?> m) {
            var map = (java.util.Map<String, Object>) m;

            Users u = new Users();
            // primitives
            Object id = map.get("id");
            if (id != null) u.setId((int) ((Number) id).longValue());
            u.setEmail((String) map.get("email"));

            // booleans
            Object verified = map.get("verified");
            if (verified != null) u.setVerified((Boolean) verified);
            Object online = map.get("online");
            if (online != null) u.setOnline((Boolean) online);
            Object deleted = map.get("deleted");
            if (deleted != null) u.setDeleted((Boolean) deleted);

            // timestamps (optional – adapt to your format or ignore for now)
            // u.setCreateAt(parseDateTime(map.get("createAt")));
            // u.setLastSeen(parseDateTime(map.get("lastSeen")));

            // searchRank if present
            Object rank = map.get("searchRank");
            if (rank != null) u.setSearchRank(((Number) rank).doubleValue());

            // nested profile
            Object prof = map.get("userProfile");
            if (prof instanceof java.util.Map<?, ?> pm) {
                var pmap = (java.util.Map<String, Object>) pm;
                UserProfile p = new UserProfile();
                p.setUsername((String) pmap.get("username"));
                p.setProfileName((String) pmap.get("profileName"));
                p.setBio((String) pmap.get("bio"));
                p.setProfileImageUrl((String) pmap.get("profileImageUrl"));
                u.setUserProfile(p);
            }

            return u;
        }
        return null;
    }

    private void setupSearchAndChatList() {

        searchResults.setCellFactory(lv -> new ListCell<>() {
            private final ImageView avatar = new ImageView();
            private final Label name = new Label();
            private final Label email = new Label();
            private final VBox labels = new VBox(name, email);
            private final HBox root = new HBox(10, avatar, labels);

            {
                avatar.setFitWidth(32);
                avatar.setFitHeight(32);
                avatar.setPreserveRatio(true);
                root.setAlignment(Pos.CENTER_LEFT);
                root.setPadding(new Insets(8));
                name.getStyleClass().add("search-name");
                email.getStyleClass().add("search-email");
            }

            @Override protected void updateItem(SearchItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                avatar.setImage(item.avatarUrl() != null ? new Image(item.avatarUrl()) : placeholder);
                name.setText(item.title());
                email.setText(item.subtitle());
                setGraphic(root);
            }
        });
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.trim().toLowerCase(Locale.ROOT);
            if (query.isEmpty()) {
                searchResults.setItems(FXCollections.observableArrayList());
                return;
            }
            SearchService searchService = new SearchServiceAdapter(NetworkService.getInstance());
            Search search = new Search(DataManager.getInstance());



            long meUserId = AppSession.requireUserId();
            List<SearchResult> results = null;
            try {
                results = search.search(query, meUserId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            System.out.println("🔍 Search returned " + results.size() + " result(s)");
            results.forEach(r -> System.out.println(" - " + r));


            List<SearchItem> userItems = convertToSearchItems(results);
            System.out.println("✅ Converted to " + userItems.size() + " SearchItem(s)");
            userItems.forEach(item -> System.out.println(" - " + item.title() + " | " + item.subtitle()));
            searchResults.setItems(FXCollections.observableArrayList(userItems));
            searchResults.setPlaceholder(new Label("Nothing to show ..."));
        });
        searchResults.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, sel) -> {
            if (sel == null) return;

            // ✅ هندل یوزرها (بدون تغییر)
            if (sel.kind() == SearchKind.USER && sel.rawEntity() instanceof Users u) {
                showUserProfile(u);
                mediaGrid.getChildren().clear();

                long meUserId = AppSession.requireUserId();
                long targetUserId = u.getId();

                Optional<ChatItem> existingItem = allChats.stream()
                        .filter(item -> item.rawEntity instanceof Users && ((Users) item.rawEntity).getId() == targetUserId)
                        .findFirst();

                if (existingItem.isPresent()) {
                    System.out.println("✅ Chat with " + u.getUserProfile().getProfileName() + " already exists. Selecting it.");
                    chatList.getSelectionModel().select(existingItem.get());
                    chatList.scrollTo(existingItem.get());
                } else {
                    System.out.println("ℹ️ Creating a new chat with " + u.getUserProfile().getProfileName());
                    ChatService net = new NetworkChatServiceAdapter(NetworkService.getInstance());
                    PrivateChat chat = net.getOrCreatePrivateChat(meUserId, targetUserId);
                    if (chat != null) {
                        String avatarUrl = u.getUserProfile().getProfileImageUrl();
                        Image avatar = (avatarUrl != null && !avatarUrl.isBlank())
                                ? new Image(avatarUrl)
                                : placeholder;

                        ChatItem newItem = ChatItem.fromContact(chat.toContact(u), avatar);
                        newItem.setRawEntity(u);
                        allChats.add(0, newItem);
                        chatList.getSelectionModel().select(newItem);
                        chatList.scrollTo(0);
                    }
                }

                searchField.clear();
                searchResults.getItems().clear();
                return;
            }

            // ✅ هندل چت‌ها (اضافه‌شده)
            if (sel.kind() == SearchKind.CHAT && sel.rawEntity() instanceof Chat c) {
                System.out.println("💬 Selecting chat from search: " + c.getChatId() + " | " + c.getName());

                Optional<ChatItem> existingItem = allChats.stream()
                        .filter(item -> item.rawEntity instanceof Chat && ((Chat) item.rawEntity).getChatId() == c.getChatId())
                        .findFirst();

                if (existingItem.isPresent()) {
                    chatList.getSelectionModel().select(existingItem.get());
                    chatList.scrollTo(existingItem.get());
                } else {
                    Contact contact = buildContactFromChat(c);
                    Image avatar = (contact.avatarUrl() != null && !contact.avatarUrl().isBlank())
                            ? new Image(contact.avatarUrl())
                            : placeholder;

                    ChatItem newItem = ChatItem.fromContact(contact, avatar);
                    newItem.setRawEntity(c);
                    allChats.add(0, newItem);
                    chatList.getSelectionModel().select(newItem);
                    chatList.scrollTo(0);
                }

                searchField.clear();
                searchResults.getItems().clear();
            }
        });
        chatList.getSelectionModel().selectedItemProperty().addListener((o, oldSel, sel) -> {
            if (sel == null) {
                System.out.println("⚠️ No chat item selected");
                return;
            }

            System.out.println("💬 Chat selected: " + sel.title + " | chatId=" + sel.contactId);
            openChat(sel);

            long meUserId = AppSession.requireUserId();
            long chatId = sel.contactId;

            try {
                System.out.println("📡 Fetching messages for chatId=" + chatId);
                List<MessageEntity> messages = NetworkService.getInstance().fetchMessages(chatId);
                System.out.println("📥 Received " + messages.size() + " messages");

                for (MessageEntity m : messages) {
                    System.out.println("🔄 Converting message: " + m.getText() + " | sender=" + m.getSender().getEmail());
                    Msg msg = new Msg(
                            m.getSender().getId() == meUserId,
                            m.getText(),
                            m.getSentAt().toLocalDateTime()
                    );
                    sel.messages.add(msg);
                }

                System.out.println("✅ Messages added to chat item: " + sel.messages.size());
                openChat(sel);
            } catch (IOException ex) {
                System.err.println("❌ Failed to load messages: " + ex.getMessage());
            }

            try {
                ServiceLocator.chat().openChat(meUserId, sel.contactId);
                System.out.println("📨 Notified server: chat opened");
            } catch (Exception ex) {
                System.err.println("[chat] openChat failed: " + ex.getMessage());
            }
        });
    }
    private Contact buildContactFromChat(Chat c) {
        String title = c.getName();
        String avatarUrl = null;
        Integer memberCount = null;
        ContactType type;

        switch (c.getType()) {
            case PRIVATE_CHAT -> type = ContactType.DIRECT;
            case GROUP_CHAT -> {
                type = ContactType.GROUP;
                if (c instanceof GroupChat g) {
                    avatarUrl = g.getGroupAvatarUrl();
                    memberCount = g.getMemberCount();
                }
            }
            case CHANNEL -> {
                type = ContactType.CHANNEL;
                if (c instanceof Channel ch) {
                    avatarUrl = ch.getChannelAvatarUrl();
                    memberCount = ch.getSubscriberCount();
                }
            }
            default -> throw new IllegalStateException("Unknown chat type: " + c.getType());
        }

        return new Contact(
                c.getChatId(),
                title,
                avatarUrl,
                false,
                type,
                memberCount
        );
    }
    private void showUserProfile(Users u) {
        UserProfile p = u.getUserProfile();

        infoAvatar.setImage(p.getProfileImageUrl() != null ? new Image(p.getProfileImageUrl()) : placeholder);
        infoName.setText(p.getProfileName() != null ? p.getProfileName() : "No name");
        infoBio.setText(p.getBio() != null ? p.getBio() : "No bio");
        infoUsername.setText(p.getUsername() != null ? "@" + p.getUsername() : "");
        infoEmail.setText(u.getEmail() != null ? u.getEmail() : "");
        infoPresence.setText(u.isOnline() ? "🟢 Online" : "⚫ Offline");
    }

    private List<SearchItem> convertToSearchItems(List<SearchResult> results) {
        return results.stream()
                .map(result -> {
                    Object entity = result.getEntity();


                    if (entity instanceof Users u) {
                        UserProfile p = u.getUserProfile();
                        String profileName = p != null ? p.getProfileName() : null;
                        String email = u.getEmail();


                        if ((profileName != null && !profileName.isBlank()) ||
                                (email != null && !email.isBlank())) {

                            return new SearchItem(
                                    profileName != null ? profileName : email,
                                    email != null ? email : "No email",
                                    p != null ? p.getProfileImageUrl() : null,
                                    SearchKind.USER,
                                    u
                            );
                        }
                        return null;
                    }


                    if (entity instanceof Chat c) {
                        String title = null;
                        String subtitle = null;
                        String avatarUrl = null;

                        if (c instanceof Channel ch) {
                            title = ch.getChannelName();
                            subtitle = "Channel";
                            avatarUrl = ch.getChannelAvatarUrl();
                        } else if (c instanceof GroupChat g) {
                            title = g.getGroupName();
                            subtitle = "Group";
                            avatarUrl = g.getGroupAvatarUrl();
                        }

                        if (title != null && !title.isBlank()) {
                            return new SearchItem(
                                    title,
                                    subtitle != null ? subtitle : "Chat",
                                    avatarUrl,
                                    SearchKind.CHAT,
                                    c
                            );
                        }
                        return null;
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .filter(item -> item.title() != null && !item.title().isBlank())
                .filter(item -> !item.title().equals("Unknown"))
                .toList();
    }
    private void showChatInfo(Chat chat) {
        if (chat instanceof Channel channel) {

            infoAvatar.setImage(channel.getChannelAvatarUrl() != null ?
                    new Image(channel.getChannelAvatarUrl()) : placeholder);
            infoName.setText(channel.getChannelName() != null ? channel.getChannelName() : "No name");
            infoBio.setText(channel.getDescription() != null ? channel.getDescription() : "No description");
            infoUsername.setText("");
            infoEmail.setText("");
            infoPresence.setText("Channel • " + channel.getSubscriberCount() + " subscribers");

        } else if (chat instanceof GroupChat group) {

            infoAvatar.setImage(group.getGroupAvatarUrl() != null ?
                    new Image(group.getGroupAvatarUrl()) : placeholder);
            infoName.setText(group.getGroupName() != null ? group.getGroupName() : "No name");
            infoBio.setText(group.getDescription() != null ? group.getDescription() : "No description");
            infoUsername.setText("");
            infoEmail.setText("");
            infoPresence.setText("Group • " + group.getMemberCount() + " members");
        }

        mediaGrid.getChildren().clear();
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
        if (text.isEmpty()) {
            return;
        }

        // Get the current active chat from chatList
        ChatItem selectedChat = chatList.getSelectionModel().getSelectedItem();
        if (selectedChat == null) {
            System.err.println("❌ No chat selected. Cannot send message.");
            return;
        }

        // Add message to message list visually and instantly (Optimistic UI Update)
        Msg newMessage = new Msg(true, text, LocalDateTime.now());
        selectedChat.messages.add(newMessage);

        // Send the message to the server
        try {
            System.out.println("🚀 Sending message to contactId: " + selectedChat.contactId);
            ServiceLocator.chat().sendMessage(selectedChat.contactId, text);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Clear the input field
        inputField.clear();
    }

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

    /*================== Public loading APIs (Stage-1) ==================
       loadChats(List<Contact> contacts)
       - PUBLIC entry point for current app flow (DIRECT chats via existing Contact DTO).
       - Converts Contact -> ChatRef(DIRECT) internally and delegates to loadChatsGeneric(...).
       - Backend/DataManager should call this after fetching user's chats by userId.
       -----* *------
    */
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
            case DIRECT  -> (c.online ? "🟢 Online" : "⚫ Last seen recently");
            case GROUP   -> (c.memberCount != null ? c.memberCount + " members" : "Group");
            case CHANNEL -> "Channel";
        };
        headerStatus.setText(status);


        if (c.rawEntity instanceof Users user) {
            showUserProfile(user);
        } else if (c.rawEntity instanceof Chat chat) {
            showChatInfo(chat);
        } else {

            infoAvatar.setImage(c.avatar != null ? c.avatar : placeholder);
            infoName.setText(c.title);
            infoPresence.setText(status);
            infoBio.setText("");
            infoUsername.setText("");
            infoEmail.setText("");
            mediaGrid.getChildren().clear();
        }

        messageList.setItems(FXCollections.observableArrayList());
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
        public final int unreadCount;// reserved badge
        public  Objects rawEntity;

        public ChatRef(long id, ChatKind kind, String title, String avatarUrl,
                       boolean online, Integer memberCount, boolean muted, int unreadCount) {
            this.id = id; this.kind = kind; this.title = title; this.avatarUrl = avatarUrl;
            this.online = online; this.memberCount = memberCount; this.muted = muted; this.unreadCount = unreadCount;
        }
    }

    public static final class ChatItem {
        public final long contactId;         // ID of the target (DIRECT/GROUP/CHANNEL)
        public final String title;
        public boolean online;               // presence (applies for DIRECT)
        public final Image avatar;
        public final ChatKind kind;
        public final Integer memberCount;    // for GROUP
        public final boolean muted;          // reserved
        public final ObservableList<Msg> messages = FXCollections.observableArrayList();
        public Object rawEntity;

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

        public Object getRawEntity() {
            return rawEntity;
        }

        public void setRawEntity(Object rawEntity) {
            this.rawEntity = rawEntity;
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

    @FXML
    private void toggleMenuOverlay(ActionEvent event) {
        menuOverlay.setVisible(true);
        menuOverlay.setManaged(true);
        scrim.setVisible(true);
        scrim.setManaged(true);
    }

    @FXML
    private void closeAllOverlays(ActionEvent event) {
        menuOverlay.setVisible(false);
        menuOverlay.setManaged(false);
        subPageOverlay.setVisible(false);
        subPageOverlay.setManaged(false);
        scrim.setVisible(false);
        scrim.setManaged(false);
    }

    @FXML
    private void backToMenu(ActionEvent event) {
        subPageOverlay.setVisible(false);
        subPageOverlay.setManaged(false);
        menuOverlay.setVisible(true);
        menuOverlay.setManaged(true);
    }
    @FXML
    private void openCreateChannel(MouseEvent event) {
        subPageTitle.setText("Create Channel");
        menuOverlay.setVisible(false);
        menuOverlay.setManaged(false);
        subPageOverlay.setVisible(true);
        subPageOverlay.setManaged(true);
    }

    @FXML
    private void openCreateGroup(MouseEvent event) {
        subPageTitle.setText("Create Group");
        menuOverlay.setVisible(false);
        menuOverlay.setManaged(false);
        subPageOverlay.setVisible(true);
        subPageOverlay.setManaged(true);
    }

    @FXML
    private void openContacts(MouseEvent event) {
        subPageTitle.setText("Contacts");
        menuOverlay.setVisible(false);
        menuOverlay.setManaged(false);
        subPageOverlay.setVisible(true);
        subPageOverlay.setManaged(true);
    }

    @FXML
    private void openSettings(MouseEvent event) {
        subPageTitle.setText("Settings");
        menuOverlay.setVisible(false);
        menuOverlay.setManaged(false);
        subPageOverlay.setVisible(true);
        subPageOverlay.setManaged(true);
    }
}
