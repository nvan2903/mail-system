package com.email.mailclient.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;

public class ProfileController extends CommonController implements Initializable {
    @FXML
    private TextField txtUserName ;
    @FXML
    private PasswordField txtPassword;
    private String loginResponse= "";
    Properties config;
    private Socket socket;
    private String emailAddress = "";
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
getUserInfor();
    }

    private void getUserInfor() {
        String[] responseParts = loginResponse.split(" ", 4);

        if (responseParts.length >= 4 && "OK".equals(responseParts[0])) {
            String username = responseParts[3];
            String password = responseParts[2];
            txtUserName.setText(username);
            txtPassword.setText(password);
        }
    }


    public ProfileController( Properties config, Socket socket, String emailAddress, String loginResponse) {
        this.config = config;
        this.socket = socket;
        this.emailAddress = emailAddress;
        this.loginResponse = loginResponse;
    }

@FXML
private void loadEdit1(){
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/email/mailclient/edit1.fxml"));
        loader.setControllerFactory(c -> new ChangeNameController(config, socket, emailAddress, loginResponse));
        Parent root = loader.load();

        // Create a new stage
        Stage profileStage = new Stage();
        profileStage.initStyle(StageStyle.DECORATED);
        profileStage.initModality(Modality.APPLICATION_MODAL);
        profileStage.setTitle("Chỉnh Sửa");

        // Set the scene
        Scene scene = new Scene(root);
        profileStage.setScene(scene);

        // Show the stage
        profileStage.show();
    } catch (IOException e) {
        e.printStackTrace(); // Handle the exception appropriately
    }
}

    @FXML
    private void loadEdit2(){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/email/mailclient/edit2.fxml"));
            loader.setControllerFactory(c -> new ChangePasswordController(config, socket, emailAddress, loginResponse));
            Parent root = loader.load();

            // Create a new stage
            Stage profileStage = new Stage();
            profileStage.initStyle(StageStyle.DECORATED);
            profileStage.initModality(Modality.APPLICATION_MODAL);
            profileStage.setTitle("Chỉnh Sửa");

            // Set the scene
            Scene scene = new Scene(root);
            profileStage.setScene(scene);

            // Show the stage
            profileStage.show();
        } catch (IOException e) {
            e.printStackTrace(); // Handle the exception appropriately
        }
    }

    @FXML
    private void handleLogout() {
        // Tạo hộp thoại xác nhận
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận đăng xuất");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất?");
        // Get the user's response when clicking OK or Cancel
        Optional<ButtonType> result = alert.showAndWait();

        // Process logout if the user clicked OK
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Create DataOutputStream and DataInputStream
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                // Send logout command to the server
                dos.writeUTF("LOGOUT");

                // Receive and print the server's response
                String response = dis.readUTF();
                System.out.println(response);

                // Close the socket
                socket.close();


                // Close the application
                Platform.exit();


            } catch (IOException e) {
                // Handle communication error
                showError("Lỗi khi giao tiếp với máy chủ: " + e.getMessage());
            }
        }


    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
