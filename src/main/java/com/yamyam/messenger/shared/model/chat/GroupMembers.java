package com.yamyam.messenger.shared.model.chat;

import com.yamyam.messenger.shared.model.user.Users;

import java.sql.Timestamp;

public class GroupMembers {

    private GroupChat groupChat;
    private Role role;
    private Users member;
    private Users invitedBy;
    private Timestamp joinedAt;

    public GroupMembers(GroupChat groupChat, Role role, Users member, Users invitedBy) {
        this.groupChat = groupChat;
        this.member = member;
        this.invitedBy = invitedBy;
        this.joinedAt = new Timestamp(System.currentTimeMillis());
    }

    public GroupChat getGroupChat() {
        return groupChat;
    }

    public void setGroupChat(GroupChat groupChat) {
        this.groupChat = groupChat;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Users getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(Users invitedBy) {
        this.invitedBy = invitedBy;
    }

    public Users getMember() {
        return member;
    }

    public void setMember(Users member) {
        this.member = member;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
    @Override
    public String toString() {
        return groupChat.getChatId() + "," +
                role.name() + "," +
                member.getId() + "," +
                invitedBy.getId() + "," +
                joinedAt.getTime();
    }
}
