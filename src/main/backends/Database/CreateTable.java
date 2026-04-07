package Database;

import java.sql.Connection;
import java.sql.Statement;

public class CreateTable {
    public static void create(){
        try{
            Connection conn=DBConnection.getConnection();
            Statement stm=conn.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT, " +
                    "phoneNumber TEXT, " +
                    "Email TEXT, " +
                    "password TEXT)";
            stm.execute(sql);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
