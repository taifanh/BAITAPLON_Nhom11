package com.bidding_system.backends.launcher;

import com.bidding_system.backends.common.models.accounts.Admin;
import com.bidding_system.backends.server.database.UserDAO;

import java.io.IOException;

public class CreateAdmin {
    static void main(String[] args) {
        Admin admin = Admin.creating_admin("Admin","admin","12345","admin");
        UserDAO userDAO = new UserDAO();
        try{
            userDAO.saveAdmin(admin);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
