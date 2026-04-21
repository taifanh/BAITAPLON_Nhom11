package controllers;

import models.accounts.User;

public final class UserSession {
    private static User currentUser;
    private static ServerConnection connection;

    private UserSession() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void initConnection(String host, int port) throws Exception {
        if (connection == null) {
            connection = new ServerConnection();
            connection.connect(host, port);
        }
    }

    public static ServerConnection getConnection() {
        return connection;
    }

    public static void clear() {
        currentUser = null;
        if (connection != null) {
            connection. disconnect();
            connection = null;
        }
    }
}
