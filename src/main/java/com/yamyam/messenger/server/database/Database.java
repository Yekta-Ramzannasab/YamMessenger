package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {

    // Static HikariDataSource, acts like a singleton for the whole app
    private static final HikariDataSource dataSource;


    static {
        // Read environment variables for database credentials
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        
        // Check if any required environment variable is missing
        if (url == null || user == null || password == null) {
            throw new RuntimeException("Database environment variables (DB_URL, DB_USER, DB_PASSWORD) are not set!");
        }

        // Configure HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);                     // Database URL
        config.setUsername(user);                   // Database username
        config.setPassword(password);               // Database password

        // Pool settings
        config.setMaximumPoolSize(10);             // Maximum number of connections in the pool
        config.setMinimumIdle(2);                  // Minimum number of idle connections kept in the pool
        config.setIdleTimeout(30000);              // 30 seconds before an idle connection is released
        config.setMaxLifetime(1800000);            // 30 minutes maximum lifetime for a connection
        config.setConnectionTimeout(10000);        // Wait up to 10 seconds for a connection

        // Initialize the connection pool
        dataSource = new HikariDataSource(config);
    }

    // Private constructor to prevent instantiation (singleton-like)
    private Database() {
    }

    /**
     * Get a connection from the pool.
     * Remember: closing the connection returns it to the pool, it does not really close it.
     *
     * @return Connection from the pool
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static Users loadUser(long userId) throws SQLException {
        // profile could be null?
        try (Connection connection = Database.getConnection()) {

            String sql = "SELECT u.*, p.* FROM users u LEFT JOIN user_profiles p ON u.user_id = p.user_id WHERE u.user_id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    // Build UserProfile object
                    UserProfile profile = new UserProfile(
                            rs.getLong("profile_id"),
                            rs.getString("profile_image_url"),
                            rs.getString("bio"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getTimestamp("updated_at"),
                            rs.getString("profile_name")
                    );

                    // Build Users object
                    Users user = new Users(
                            rs.getLong("user_id"),
                            rs.getTimestamp("created_at"),
                            rs.getTimestamp("last_seen"),
                            rs.getBoolean("is_verified"),
                            rs.getBoolean("is_online"),
                            rs.getBoolean("is_deleted"),
                            rs.getString("email"),
                            profile
                    );

                    return user;
                } else {
                    return null;
                }
            }
        }
    }

    public static List<Users> loadAllUsers() throws SQLException {
        try (Connection connection = Database.getConnection()) {
            String sql = "SELECT u.user_id, u.created_at, u.last_seen, u.is_verified, u.is_online, u.is_deleted, u.email, " +
                    "p.profile_id, p.profile_image_url, p.bio, p.username, p.password, p.updated_at, p.profile_name " +
                    "FROM users u " +
                    "LEFT JOIN user_profiles p ON u.user_id = p.user_id;";

            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rs = statement.executeQuery();
            List<Users> usersList = new ArrayList<>();
            while (rs.next()) {
                UserProfile profile = null;
                if (rs.getLong("profile_id") != 0) {
                    profile = new UserProfile(
                            rs.getLong("profile_id"),
                            rs.getString("profile_image_url"),
                            rs.getString("bio"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getTimestamp("updated_at"),
                            rs.getString("profile_name")
                    );
                }

                Users user = new Users(
                        rs.getLong("user_id"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("last_seen"),
                        rs.getBoolean("is_verified"),
                        rs.getBoolean("is_online"),
                        rs.getBoolean("is_deleted"),
                        rs.getString("email"),
                        profile
                );

                usersList.add(user);
            }
            return usersList;
        }
    }

    public static long getUserIdByEmail(String email) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            String sql = "SELECT user_id FROM users WHERE email = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, email);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getLong("user_id");
            }

        }
        return -1;
    }


    // ----- Messages -----
    public static List<MessageEntity> loadMessages(long chatId) throws SQLException {
        List<MessageEntity> messages = new ArrayList<>();

        try (Connection connection = Database.getConnection()) {
            String sql = "SELECT m.message_id, m.chat_id, m.sender_id, m.message_text, m.message_type, " +
                    "m.reply, m.forward, m.is_edited, m.is_deleted, m.status, m.sent_at, " +
                    "u.user_id, u.email, u.is_online, u.is_verified, u.is_deleted, " +
                    "p.profile_id, p.username, p.profile_image_url " +
                    "FROM messages m " +
                    "LEFT JOIN users u ON m.sender_id = u.user_id " +
                    "LEFT JOIN user_profiles p ON u.user_id = p.user_id " +
                    "WHERE m.chat_id = ?";

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setLong(1, chatId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                // Build UserProfile
                UserProfile profile = null;
                if (rs.getLong("profile_id") != 0) {
                    profile = new UserProfile(
                            rs.getLong("profile_id"),
                            rs.getString("profile_image_url"),
                            null, // bio
                            rs.getString("username"),
                            null, // password
                            null, // updated_at
                            null  // profile_name
                    );
                }

                // Build Users
                Users sender = new Users(
                        rs.getLong("user_id"),
                        null, // created_at
                        null, // last_seen
                        rs.getBoolean("is_verified"),
                        rs.getBoolean("is_online"),
                        rs.getBoolean("is_deleted"),
                        rs.getString("email"),
                        profile
                );

                Chat chat = new PrivateChat(rs.getLong("chat_id"), 0, 0);

                // Build MessageEntity
                MessageEntity message = new MessageEntity(
                        MessageType.valueOf(rs.getString("status")),
                        rs.getBoolean("is_deleted"),
                        rs.getBoolean("is_edited"),
                        rs.getLong("forward"),
                        rs.getLong("reply"),
                        MessageType.valueOf(rs.getString("message_type")),
                        rs.getString("message_text"),
                        chat,
                        sender,
                        rs.getLong("message_id")
                );

                messages.add(message);
            }
        }

        return messages;
    }


    // ----- Chats -----
    public static PrivateChat loadPrivateChat(long chatId) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            String sql = "SELECT chat_id, user1_id, user2_id, created_at " +
                    "FROM private_chats " +
                    "WHERE chat_id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        PrivateChat chat = new PrivateChat(
                                rs.getLong("chat_id"),
                                rs.getLong("user1_id"),
                                rs.getLong("user2_id")
                        );
                        chat.setCreatedAt(rs.getTimestamp("created_at"));
                        return chat;
                    } else {
                        return null;
                    }
                }
            }
        }
    }
    public static PrivateChat loadPrivateChat(long userA, long userB) throws SQLException {
        long user1 = Math.min(userA, userB);
        long user2 = Math.max(userA, userB);

        try (Connection connection = Database.getConnection()) {
            String sql = "SELECT chat_id, user1_id, user2_id, created_at " +
                    "FROM private_chats " +
                    "WHERE user1_id = ? AND user2_id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, user1);
                stmt.setLong(2, user2);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        PrivateChat chat = new PrivateChat(
                                rs.getLong("chat_id"),
                                rs.getLong("user1_id"),
                                rs.getLong("user2_id")
                        );
                        chat.setCreatedAt(rs.getTimestamp("created_at"));
                        return chat;
                    } else {
                        return null;
                    }
                }
            }
        }
    }
    public static PrivateChat createPrivateChat(long userA, long userB) throws SQLException {
        long user1 = Math.min(userA, userB);
        long user2 = Math.max(userA, userB);

        try (Connection connection = Database.getConnection()) {
            String sql = "INSERT INTO private_chats(user1_id, user2_id) VALUES (?, ?) RETURNING chat_id, created_at";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, user1);
                stmt.setLong(2, user2);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long chatId = rs.getLong("chat_id");
                        Timestamp createdAt = rs.getTimestamp("created_at");

                        PrivateChat chat = new PrivateChat(chatId, user1, user2);
                        chat.setCreatedAt(createdAt);
                        return chat;
                    } else {
                        return null;
                    }
                }
            }
        }
    }

    public static GroupChat loadGroupChat(long chatId) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            String sql = "SELECT group_id, group_name, description, creator_id, is_private, created_at " +
                    "FROM group_chats WHERE group_id = ?";

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setLong(1, chatId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                GroupChat groupChat = new GroupChat(
                        rs.getLong("group_id"),
                        rs.getString("group_name"),
                        rs.getString("description"),
                        rs.getLong("creator_id"),
                        rs.getBoolean("is_private"),
                        rs.getString("group_avatar_url")
                );

                groupChat.setCreatedAt(rs.getTimestamp("created_at"));

                return groupChat;
            }
        }

        return null;
    }
    public static GroupMembers loadGroupMember(long chatId, long userId, GroupChat groupChat, Users member, Users invitedBy) throws SQLException {
        try (Connection con = Database.getConnection()) {
            String sql = "SELECT role FROM group_members WHERE chat_id = ? AND user_id = ?";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                stmt.setLong(2, userId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    Role role = Role.valueOf(rs.getString("role").toUpperCase());
                    return new GroupMembers(groupChat, role, member, invitedBy);
                }
            }
        }
        return null;
    }
    public static GroupMembers insertGroupMember(long chatId, long userId, GroupChat groupChat, Users member, Users invitedBy) throws SQLException {
        try (Connection con = Database.getConnection()) {
            String sql = "INSERT INTO group_members(chat_id, user_id, role, joined_at, invited_by) " +
                    "VALUES (?, ?, 'member', now(), ?) RETURNING role";

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                stmt.setLong(2, userId);
                stmt.setLong(3, invitedBy.getId());

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    Role role = Role.valueOf(rs.getString("role").toUpperCase());
                    return new GroupMembers(groupChat, role, member, invitedBy);
                }
            }
        }
        throw new SQLException("Failed to insert group member");
    }


    public static Channel loadChannel(long chatId) throws SQLException {
        try (Connection con = Database.getConnection()) {
            String sql = "SELECT chat_id, channel_name, owner_id, is_private, description, created_at " +
                    "FROM channels WHERE chat_id = ?";

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    Channel channel = new Channel(
                            rs.getLong("chat_id"),
                            rs.getString("channel_name"),
                            rs.getLong("owner_id"),
                            rs.getBoolean("is_private"),
                            rs.getString("description"),
                            rs.getString("avatar_url")
                    );
                    channel.setCreatedAt(rs.getTimestamp("created_at"));
                    return channel;
                }
            }
        }
        return null;
    }
    public static Channel insertChannel(String name, long ownerId, boolean isPrivate, String description) throws SQLException {
        long chatId = createChat("CHANNEL");

        try (Connection con = Database.getConnection()) {
            String sql = "INSERT INTO channels(chat_id, channel_name, owner_id, is_private, description, avatar_url) " +
                    "VALUES (?, ?, ?, ?, ?, ?) RETURNING created_at";

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                stmt.setString(2, name);
                stmt.setLong(3, ownerId);
                stmt.setBoolean(4, isPrivate);
                stmt.setString(5, description);
                stmt.setString(6, null); // default avatar is null

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    Channel channel = new Channel(chatId, name, ownerId, isPrivate, description, null);
                    channel.setCreatedAt(rs.getTimestamp("created_at"));
                    return channel;
                }
            }
        }

        throw new SQLException("Failed to insert channel");
    }
    public static ChannelSubscribers loadSubscription(long chatId, long userId, Channel channel) throws SQLException {
        try (Connection con = Database.getConnection()) {
            String sql = "SELECT role, is_approved FROM channel_subscribers WHERE user_id = ? AND chat_id = ?";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setLong(1, userId);
                stmt.setLong(2, chatId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    Role role = Role.valueOf(rs.getString("role").toUpperCase());
                    boolean approved = rs.getBoolean("is_approved");
                    return new ChannelSubscribers(channel, role, userId, approved);
                }
            }
        }
        return null;
    }
    public static ChannelSubscribers insertSubscription(long chatId, long userId, Channel channel) throws SQLException {
        try (Connection con = Database.getConnection()) {
            String sql = "INSERT INTO channel_subscribers(chat_id, user_id, role, joined_at, is_approved) " +
                    "VALUES (?, ?, 'member', now(), true) RETURNING role, is_approved";

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                stmt.setLong(2, userId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    Role role = Role.valueOf(rs.getString("role").toUpperCase());
                    boolean approved = rs.getBoolean("is_approved");
                    return new ChannelSubscribers(channel, role, userId, approved);
                }
            }
        }
        throw new SQLException("Failed to insert subscription");
    }
    public static long createChat(String chatType) throws SQLException {
        try (Connection con = Database.getConnection()) {
            String sql = "INSERT INTO chat(chat_type) VALUES (?) RETURNING chat_id";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, chatType);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getLong("chat_id");
                }
            }
        }
        throw new SQLException("Failed to create chat");
    }

    public static List<Chat> loadUserChats(long userId) throws SQLException {
        List<Chat> userChats = new ArrayList<>();
        int counter = 0;
        try (Connection connection = Database.getConnection()) {
            // --- Load Private Chats ---
            String sqlPrivate = "SELECT chat_id, user1, user2, created_at FROM private_chats " +
                    "WHERE user1 = ? OR user2 = ?";
            PreparedStatement stmtPrivate = connection.prepareStatement(sqlPrivate);
            stmtPrivate.setLong(1, userId);
            stmtPrivate.setLong(2, userId);
            ResultSet rsPrivate = stmtPrivate.executeQuery();
            if (rsPrivate.next()) {
                while (rsPrivate.next()) {
                    PrivateChat pc = new PrivateChat(
                            rsPrivate.getLong("chat_id"),
                            rsPrivate.getLong("user1"),
                            rsPrivate.getLong("user2")
                    );
                    pc.setCreatedAt(rsPrivate.getTimestamp("created_at"));
                    userChats.add(pc);
                }
            } else {
                counter++;
            }

            // --- Load Group Chats ---
            String sqlGroup = "SELECT gc.group_id, gc.group_name, gc.description, gc.creator_id, gc.is_private, gc.created_at " +
                    "FROM group_chats gc " +
                    "JOIN group_members gm ON gc.group_id = gm.group_id " +
                    "WHERE gm.user_id = ?";
            PreparedStatement stmtGroup = connection.prepareStatement(sqlGroup);
            stmtGroup.setLong(1, userId);
            ResultSet rsGroup = stmtGroup.executeQuery();
            if (rsGroup.next()) {
                while (rsGroup.next()) {
                    GroupChat gc = new GroupChat(
                            rsGroup.getLong("group_id"),
                            rsGroup.getString("group_name"),
                            rsGroup.getString("description"),
                            rsGroup.getLong("creator_id"),
                            rsGroup.getBoolean("is_private"),
                            rsGroup.getString("group_avatar_url")
                    );
                    gc.setCreatedAt(rsGroup.getTimestamp("created_at"));
                    userChats.add(gc);
                }
            } else {
                counter++;
            }

            // --- Load Channels ---
            String sqlChannel = "SELECT channel_id, channel_name, owner, is_private, description, created_at " +
                    "FROM channels WHERE owner = ?";
            PreparedStatement stmtChannel = connection.prepareStatement(sqlChannel);
            stmtChannel.setLong(1, userId);
            ResultSet rsChannel = stmtChannel.executeQuery();
            if (rsChannel.next()) {
                while (rsChannel.next()) {
                    Channel ch = new Channel(
                            rsChannel.getLong("channel_id"),
                            rsChannel.getString("channel_name"),
                            rsChannel.getLong("owner"),
                            rsChannel.getBoolean("is_private"),
                            rsChannel.getString("description"),
                            rsChannel.getString("avatar_url")
                    );
                    ch.setCreatedAt(rsChannel.getTimestamp("created_at"));
                    userChats.add(ch);
                }
            } else {
                counter++;
            }
            if (counter == 3) {
                return null;
            } else {
                return userChats;
            }
        }
    }
    // ---- User Search ----
    public static List<Users> searchUsers(String query) throws SQLException {
        List<Users> results = new ArrayList<>();
        String sql = "SELECT u.*, p.*, ts_rank_cd(p.search_vector, plainto_tsquery('simple', ?)) AS rank " +
                "FROM users u " +
                "LEFT JOIN user_profiles p ON u.user_id = p.user_id " +
                "WHERE p.search_vector @@ plainto_tsquery('simple', ?) " +
                "ORDER BY rank DESC LIMIT 20";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, query);
            stmt.setString(2, query);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Users user = buildUserFromResultSet(rs);
                user.setSearchRank(rs.getDouble("rank"));
                results.add(user);
            }
        }
        return results;
    }


    // ---- Chat Search (Group + Channel) ----
    public static List<MessageEntity> searchMessages(String query, long userId) throws SQLException {
        List<MessageEntity> results = new ArrayList<>();
        String sql = "SELECT m.*, ts_rank_cd(m.search_vector, plainto_tsquery('simple', ?)) AS rank\n" +
                "FROM messages m\n" +
                "JOIN chat c ON m.chat_id = c.chat_id\n" +
                "LEFT JOIN private_chat pc ON c.chat_id = pc.chat_id\n" +
                "LEFT JOIN group_chat gc ON c.chat_id = gc.chat_id\n" +
                "LEFT JOIN channel ch ON c.chat_id = ch.chat_id\n" +
                "LEFT JOIN channel_subscribers cs ON c.chat_id = cs.chat_id\n" +
                "WHERE m.search_vector @@ plainto_tsquery('simple', ?)\n" +
                "AND (\n" +
                "    pc.user1_id = ? OR pc.user2_id = ?\n" +
                "    OR gc.creator_id = ?\n" +
                "    OR cs.user_id = ?\n" +
                ")\n" +
                "ORDER BY rank DESC\n" +
                "LIMIT 30";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, query);
            stmt.setString(2, query);
            stmt.setLong(3, userId);
            stmt.setLong(4, userId);
            stmt.setLong(5, userId);
            stmt.setLong(6, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                MessageEntity m = buildMessageFromResultSet(rs);
                m.setSearchRank(rs.getDouble("rank"));
                results.add(m);
            }
        }
        return results;
    }


    public static List<Chat> searchChats(String query, long userId) throws SQLException {
        List<Chat> results = new ArrayList<>();
        String sql = "SELECT c.*, ts_rank_cd(c.search_vector, plainto_tsquery('simple', ?)) AS rank " +
                "FROM chat c " +
                "LEFT JOIN channel_subscribers cs ON c.chat_id = cs.chat_id " +
                "WHERE c.search_vector @@ plainto_tsquery('simple', ?) " +
                "ORDER BY rank DESC LIMIT 20";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, query);
            stmt.setString(2, query);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Chat chat = buildChatFromResultSet(rs);
                chat.setSearchRank(rs.getDouble("rank"));
                results.add(chat);
            }
        }
        return results;
    }
    public static List<PrivateChat> getPrivateChatsByUserId(long userId) throws SQLException {
        List<PrivateChat> privateChats = new ArrayList<>();

        String sql = "SELECT chat_id, user1_id, user2_id FROM private_chat " +
                "WHERE user1_id = ? OR user2_id = ?";
        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, userId);
                stmt.setLong(2, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        long chatId = rs.getLong("chat_id");
                        long user1 = rs.getLong("user1_id");
                        long user2 = rs.getLong("user2_id");

                        // create PrivateChat
                        PrivateChat privateChat = new PrivateChat(chatId, user1, user2);
                        privateChats.add(privateChat);
                    }
                }
            }
        }

        return privateChats;
    }
    public static List<Chat> getUserGroupsAndChannels(long userId) throws SQLException {
        List<Chat> chats = new ArrayList<>();

        String groupSql = "SELECT g.chat_id, g.group_name, g.description, g.creator_id, g.is_private " +
                "FROM group_chat g " +
                "JOIN group_member gm ON g.chat_id = gm.group_id " +
                "WHERE gm.user_id = ?";
        try(Connection connection = Database.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(groupSql)) {
                stmt.setLong(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        GroupChat groupChat = new GroupChat(
                                rs.getLong("chat_id"),
                                rs.getString("group_name"),
                                rs.getString("description"),
                                rs.getLong("creator_id"),
                                rs.getBoolean("is_private"),
                                rs.getString("group_avatar_url")
                        );
                        chats.add(groupChat);
                    }
                }
            }
        }

        String channelSql = "SELECT c.chat_id, c.channel_name, c.owner, c.is_private, c.description " +
                "FROM channel c " +
                "JOIN channel_subscribers cs ON c.chat_id = cs.channel_id " +
                "WHERE cs.user_id = ?";
        try(Connection connection = Database.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(channelSql)) {
                stmt.setLong(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Channel channel = new Channel(
                                rs.getLong("chat_id"),
                                rs.getString("channel_name"),
                                rs.getLong("owner"),
                                rs.getBoolean("is_private"),
                                rs.getString("description"),
                                rs.getString("avatar_url")
                        );
                        chats.add(channel);
                    }
                }
            }
        }

        return chats;
    }
    public static List<Chat> getAllChatsByUserId(long userId) throws SQLException {
        List<Chat> allChats = new ArrayList<>();

        // --- Private Chats ---
        String privateSql = "SELECT chat_id, user1_id, user2_id FROM private_chat WHERE user1_id = ? OR user2_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement stmt = connection.prepareStatement(privateSql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long chatId = rs.getLong("chat_id");
                    long user1 = rs.getLong("user1_id");
                    long user2 = rs.getLong("user2_id");

                    allChats.add(new PrivateChat(chatId, user1, user2));
                }
            }
        }

        // --- Group Chats ---
        String groupSql = "SELECT g.chat_id, g.group_name, g.description, g.creator_id, g.is_private " +
                "FROM group_chat g " +
                "JOIN group_member gm ON g.chat_id = gm.group_id " +
                "WHERE gm.user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement stmt = connection.prepareStatement(groupSql)) {

            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    GroupChat groupChat = new GroupChat(
                            rs.getLong("chat_id"),
                            rs.getString("group_name"),
                            rs.getString("description"),
                            rs.getLong("creator_id"),
                            rs.getBoolean("is_private"),
                            rs.getString("group_avatar_url")
                    );
                    allChats.add(groupChat);
                }
            }
        }

        // --- Channels ---
        String channelSql = "SELECT c.chat_id, c.channel_name, c.owner, c.is_private, c.description " +
                "FROM channel c " +
                "JOIN channel_subscribers cs ON c.chat_id = cs.channel_id " +
                "WHERE cs.user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement stmt = connection.prepareStatement(channelSql)) {

            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Channel channel = new Channel(
                            rs.getLong("chat_id"),
                            rs.getString("channel_name"),
                            rs.getLong("owner"),
                            rs.getBoolean("is_private"),
                            rs.getString("description"),
                            rs.getString(" avatar_url ")
                    );
                    allChats.add(channel);
                }
            }
        }

        return allChats;
    }



    public static Users buildUserFromResultSet(ResultSet rs) throws SQLException {
        UserProfile profile = null;
        if (rs.getLong("profile_id") != 0) {
            profile = new UserProfile(
                    rs.getLong("profile_id"),
                    rs.getString("profile_image_url"),
                    rs.getString("bio"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getTimestamp("updated_at"),
                    rs.getString("profile_name")
            );
        }

        return new Users(
                rs.getLong("user_id"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("last_seen"),
                rs.getBoolean("is_verified"),
                rs.getBoolean("is_online"),
                rs.getBoolean("is_deleted"),
                rs.getString("email"),
                profile
        );
    }

    public static Chat buildChatFromResultSet(ResultSet rs) throws SQLException {
        Chat chat;
        if ("PRIVATE_CHAT".equals(rs.getString("chat_type"))) {
            chat = new PrivateChat(
                    rs.getLong("chat_id"),
                    rs.getLong("user1"),
                    rs.getLong("user2")
            );
        } else {
            chat = new Channel(
                    rs.getLong("chat_id"),
                    rs.getString("channel_name"),
                    rs.getLong("owner"),
                    rs.getBoolean("is_private"),
                    rs.getString("description"),
                    rs.getString("avatar_url")
            );
        }
        return chat;
    }

    public static MessageEntity buildMessageFromResultSet(ResultSet rs) throws SQLException {
        Chat chat = new PrivateChat(rs.getLong("chat_id"), 0, 0);
        Users sender = new Users(rs.getLong("sender_id"), null, null, false, false, false, null, null);
        return new MessageEntity(
                MessageType.valueOf(rs.getString("status")),
                rs.getBoolean("is_deleted"),
                rs.getBoolean("is_edited"),
                rs.getLong("forward"),
                rs.getLong("reply"),
                MessageType.valueOf(rs.getString("message_type")),
                rs.getString("message_text"),
                chat,
                sender,
                rs.getLong("message_id")
        );
    }
    public static GroupChat insertGroupChat(String name, String description, long creatorId, boolean isPrivate) throws SQLException {
        long chatId = createChat("GROUP_CHAT");

        try (Connection con = Database.getConnection()) {
            String sql = "INSERT INTO groups(chat_id, group_name, description, creator_id, created_at, is_private, group_avatar_url) " +
                    "VALUES (?, ?, ?, ?, now(), ?, ?) RETURNING created_at";

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setLong(1, chatId);
                stmt.setString(2, name);
                stmt.setString(3, description);
                stmt.setLong(4, creatorId);
                stmt.setBoolean(5, isPrivate);
                stmt.setString(6, null); // default avatar is null

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    GroupChat group = new GroupChat(chatId, name, description, creatorId, isPrivate, null);
                    group.setCreatedAt(rs.getTimestamp("created_at"));
                    return group;
                }
            }
        }

        throw new SQLException("Failed to insert group chat");
    }

}
