package controllers;

import Database.CreateTable;

import java.util.Scanner;

public class testDB {
    static void main(String[] args) {
        CreateTable.create();
        Register.register("Sanh","0963750807","25021634@vnu.edu.vn","12345678");
    }
}
