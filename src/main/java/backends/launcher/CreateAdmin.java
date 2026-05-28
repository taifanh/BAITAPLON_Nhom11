package backends.launcher;

import backends.common.models.accounts.Admin;
import backends.server.database.UserDAO;

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
