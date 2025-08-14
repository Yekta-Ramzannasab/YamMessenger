package com.yamyam.messenger.client.gui.theme;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

public final class ThemeManager {
    public enum Theme { LIGHT, DARK, AMOLED }

    private static final String BASE   = "/com/yamyam/messenger/client/gui/styles/theme-base.css";
    private static final String LIGHT  = "/com/yamyam/messenger/client/gui/styles/theme-light.css";
    private static final String DARK   = "/com/yamyam/messenger/client/gui/styles/theme-dark.css";
    private static final String AMOLED = "/com/yamyam/messenger/client/gui/styles/theme-amoled.css";

    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String KEY = "app_theme";
    private static Theme current = Theme.DARK; // پیش‌فرض

    static {
        try { current = Theme.valueOf(prefs.get(KEY, Theme.DARK.name())); } catch (Exception ignored) {}
    }

    public static void apply(Scene scene, Theme t) {
        current = t;
        if (!scene.getStylesheets().contains(BASE)) scene.getStylesheets().add(0, BASE);
        scene.getStylesheets().removeAll(LIGHT, DARK, AMOLED);
        switch (t) {
            case LIGHT -> scene.getStylesheets().add(1, LIGHT);
            case DARK  -> scene.getStylesheets().add(1, DARK);
            case AMOLED-> scene.getStylesheets().add(1, AMOLED);
        }
        prefs.put(KEY, t.name());
    }

    public static void reapply(Scene scene) { apply(scene, current); }
    public static Theme current() { return current; }
    private ThemeManager() {}
}
