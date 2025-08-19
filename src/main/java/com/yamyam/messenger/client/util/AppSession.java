package com.yamyam.messenger.client.util;

import com.yamyam.messenger.shared.model.UserProfile;

public final class AppSession {
    private static volatile UserProfile current;

    private AppSession(){}

    public static void setCurrentUser(UserProfile u) {
        current = u;
    }
    public static UserProfile getCurrentUser() {
        return current;
    }
    public static boolean isLoggedIn() {
        return current != null;
    }

    public static void clear() {
        current = null;
    }

    public static long requireUserId() {
        var u = current;
        if (u == null) throw new IllegalStateException("No logged-in user in session");
        return u.getUserId();
    }
}
