package controllers;

import Database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Login {
    public static boolean login(String Email,String password){
        try{
            Connection conn= DBConnection.getConnection();
            String sql="SELECT * FROM user WHERE Email=? AND password=?";
            PreparedStatement ps=conn.prepareStatement(sql);

            ps.setString(1,Email);
            ps.setString(2,password);

            ResultSet res=ps.executeQuery();

            return res.next();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
