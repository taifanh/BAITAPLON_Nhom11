package Database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    public static Connection getConnection(){
        try{
            String url="jdbc:sqlite:users.db";
            return DriverManager.getConnection(url);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
