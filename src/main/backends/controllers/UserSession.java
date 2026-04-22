package controllers;

import models.accounts.User;

public final class UserSession {
    private static User currentUser;

    public UserSession() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }
}
