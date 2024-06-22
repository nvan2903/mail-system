package com.email.mailclient.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;

public class ChangePasswordController extends CommonController implements Initializable {
    @FXML
    private TextField txtOldInfo;
    @FXML
    private TextField txtNewInfo;
    @FXML
    private Button btnSave2;
    private String loginResponse= "";
    Properties config;
    private Socket socket;
    private String emailAddress = "";
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public ChangePasswordController(Properties config, Socket socket, String emailAddress, String loginResponse) {
        this.config = config;
        this.socket = socket;
        this.emailAddress = emailAddress;
        this.loginResponse = loginResponse;
    }

    @FXML
    private void saveChange() {
        String oldPass = txtOldInfo.getText();
        String newPass = txtNewInfo.getText();
        if (oldPass.isEmpty() || newPass.isEmpty()) {
            showError("Vui lòng đièn đầy đủ các trường!");
            return;
        }
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            dos.writeUTF("CHPASS "+ oldPass + " " + newPass);
            String Response = dis.readUTF();
            System.out.println(Response);
            if (Response.equals("OK")) {
                showSuccessAlert("Đã Lưu Cập Nhật. Đăng nhập lại để xem thay đổi.");
                // Close the login window when switching to the main screen
                Stage edit1Stage = (Stage) btnSave2.getScene().getWindow();
                edit1Stage.close();

            }else {
                showError("Kiểm tra lại thông tin nhập vào.");
            }

        } catch (IOException e) {
            showError("Lỗi khi giao tiếp với máy chủ: " + e.getMessage());
        }


    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
