package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.chat.GroupChat;
import com.yamyam.messenger.shared.model.chat.GroupMembers;
import com.yamyam.messenger.shared.model.user.Users;

import java.sql.SQLException;

public class GroupMembersHandler {

    private static final GroupMembersHandler instance = new GroupMembersHandler();

    public static GroupMembersHandler getInstance() {
        return instance;
    }

    private GroupMembersHandler() {}

    public GroupMembers JoinUser(GroupChat groupChat, Users member, Users invitedBy) {
        try {
            GroupMembers existing = Database.loadGroupMember(groupChat.getChatId(), member.getId());
            if (existing != null) return existing;

            return Database.insertGroupMember(groupChat.getChatId(), member.getId(), groupChat, member, invitedBy);
        } catch (SQLException e) {
            throw new RuntimeException("Error in checkOrJoinUser", e);
        }
    }
}