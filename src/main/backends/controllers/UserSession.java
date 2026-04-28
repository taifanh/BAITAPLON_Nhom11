package controllers;

import models.core.Account;
import models.accounts.User;

public final class UserSession {
    private static Account currentAccount;
    private static ServerConnection connection;

    private UserSession() {
    }

    public static Account getCurrentAccount() {
        return currentAccount;
    }

    public static User getCurrentUser() {
        return currentAccount instanceof User user ? user : null;
    }

    public static void setCurrentAccount(Account account) {
        currentAccount = account;
    }

    public static void setCurrentUser(User user) {
        currentAccount = user;
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
        currentAccount = null;
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }
}
