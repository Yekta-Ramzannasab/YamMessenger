package com.yamyam.messenger.server.database;

import com.yamyam.messenger.shared.model.Contact;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ContactHandler {

    public void addContact(long ownerId, long contactId) throws SQLException, SQLException {
        String sql = "INSERT INTO contacts (owner_id, contact_id) VALUES (?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ownerId);
            stmt.setLong(2, contactId);
            stmt.executeUpdate();
        }
    }

    public List<Contact> getContacts(long ownerId) throws SQLException {
        List<Contact> contacts = new ArrayList<>();
        String sql = "SELECT * FROM contacts WHERE owner_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ownerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Contact contact = new Contact(
                            rs.getLong("owner_id"),
                            rs.getLong("contact_id"),
                            rs.getTimestamp("added_at").toLocalDateTime()
                    );
                    contacts.add(contact);
                }
            }
        }
        return contacts;
    }
}