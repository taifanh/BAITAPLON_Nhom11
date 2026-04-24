package Database;

import java.sql.Connection;
import java.sql.Statement;

public class CreateTable {
    public static void create(){
        try{
            Connection conn=DBConnection.getConnection();
            Statement stm=conn.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "phone_number TEXT NOT NULL UNIQUE, " +
                    "email TEXT NOT NULL, " +
                    "password TEXT NOT NULL, " +
                    "balance DOUBLE)";
            stm.execute(sql);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
