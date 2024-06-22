package com.vku.email;

import javafx.scene.control.Alert;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ConnectDB {
    public ConnectDB() {}

    public Connection getConnection() throws SQLException {
        Connection conn = null;
        try {
            String dbName = "mail";
            String url = "jdbc:mysql://localhost/" + dbName;
            Class.forName("com.mysql.cj.jdbc.Driver");
            String user = "root";
            String pass = "";
            conn = DriverManager.getConnection(url, user, pass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return conn;
    }
}