package controllers;

import models.core.Account;

public final class UserSession {
    private static Account currentUser;

    private UserSession() {
    }

    public static Account getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(Account user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }
}
