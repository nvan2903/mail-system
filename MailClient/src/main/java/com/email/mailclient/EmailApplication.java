package com.email.mailclient;

import com.email.mailclient.controllers.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

public class EmailApplication extends Application {
    private Properties config;
    private Socket socket;
    public EmailApplication() {

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Email Client");

            loadLoginScreen(primaryStage);

    }


    private void loadLoginScreen(Stage primaryStage) {
        try {
            // Tải tệp FXML cho giao diện đăng nhập
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/email/mailclient/login.fxml"));
            loader.setControllerFactory(c -> new LoginController());
            Parent root = loader.load();

            // Tạo một Scene với giao diện đã tải
            Scene scene = new Scene(root);

            // Đặt Scene cho Stage chính
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Lỗi khi tải giao diện đăng nhập: " + e.getMessage());
            // Bạn có thể xử lý lỗi này theo cách cần thiết cho ứng dụng của bạn.
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
