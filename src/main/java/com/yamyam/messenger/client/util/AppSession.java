package com.yamyam.messenger.client.util;

import com.yamyam.messenger.shared.model.UserProfile;
import com.yamyam.messenger.shared.model.Users;

import java.util.concurrent.atomic.AtomicReference; // این import را اضافه کن

public final class AppSession {

    // Atomic Reference is Thread Safety
    private static final AtomicReference<Users> currentUser = new AtomicReference<>();

    private AppSession() {}

    public static void setCurrentUser(Users u) {
        // use the atomic set method
        currentUser.set(u);
    }

    public static Users getCurrentUser() {
        // use the atomic get method
        return currentUser.get();
    }

    public static boolean isLoggedIn() {
        return currentUser.get() != null;
    }

    public static void clear() {
        currentUser.set(null);
    }

    public static long requireUserId() {
        Users u = currentUser.get();
        if (u == null) {
            throw new IllegalStateException("No logged-in user in session");
        }
        return u.getId();
    }
}
