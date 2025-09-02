package com.yamyam.messenger.client.network.api;

import com.yamyam.messenger.shared.model.Users;
import java.util.List;

public interface UserService {
    List<Users> getAllUsers();
    Users getUserById(long userId);
}