package com.example.desarrooapps;

import android.content.Context;
import android.content.SharedPreferences;

final class SessionManager {
    private static final String PREFERENCES_NAME = "app_session";
    private static final String KEY_USERNAME = "nombreUsuario";
    private final SharedPreferences preferences;

    SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    String getUsername() {
        String username = preferences.getString(KEY_USERNAME, null);
        return username == null || username.trim().isEmpty() ? null : username;
    }

    void saveUsername(String username) {
        preferences.edit().putString(KEY_USERNAME, username).apply();
    }

    void clear() {
        preferences.edit().remove(KEY_USERNAME).apply();
    }
}
