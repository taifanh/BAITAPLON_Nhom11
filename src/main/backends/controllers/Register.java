package controllers;

import Database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class Register {
    public static void register(String name, String Email , String phoneNumber,String password){
        try{
            Connection conn= DBConnection.getConnection();
            String sql="INSERT INTO users(name,Email , phoneNumber,password) VALUES(?,?,?,?)";
            PreparedStatement ps=conn.prepareStatement(sql);
            ps.setString(1,name);
            ps.setString(2,Email);
            ps.setString(3,phoneNumber);
            ps.setString(4,password);
            ps.executeUpdate();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
