package com.yamyam.messenger.client.util;

import com.yamyam.messenger.shared.model.UserProfile;
import java.util.concurrent.atomic.AtomicReference; // این import را اضافه کن

public final class AppSession {

    // Atomic Reference is Thread Safety
    private static final AtomicReference<UserProfile> currentUser = new AtomicReference<>();

    private AppSession() {}

    public static void setCurrentUser(UserProfile u) {
        // use the atomic set method
        currentUser.set(u);
    }

    public static UserProfile getCurrentUser() {
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
        UserProfile u = currentUser.get();
        if (u == null) {
            throw new IllegalStateException("No logged-in user in session");
        }
        return u.getUserId();
    }
}
