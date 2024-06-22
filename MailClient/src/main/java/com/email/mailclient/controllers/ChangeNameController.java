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

public class ChangeNameController extends CommonController implements Initializable {
    private String loginResponse = "";
    private String fullname = "";
    @FXML
    private TextField txtOldInfo;
    @FXML
    private TextField txtNewInfo;
    @FXML
    private Button btnSave1;
    Properties config;
    private Socket socket;
    private String emailAddress = "";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setUserName();
    }

    private void setUserName() {
        String[] responseParts = loginResponse.split(" ", 4);

        if (responseParts.length >= 4 && "OK".equals(responseParts[0])) {
            String username = responseParts[3];
            this.fullname = username;
            txtOldInfo.setText(fullname);
        }
    }

    public ChangeNameController(Properties config, Socket socket, String emailAddress, String loginResponse) {
        this.config = config;
        this.socket = socket;
        this.emailAddress = emailAddress;
        this.loginResponse = loginResponse;
    }

    @FXML
    private void saveChange() {
        String newFullName = txtNewInfo.getText();
        if (newFullName.isEmpty()) {
            showError("Vui lòng đièn đầy đủ các trường!");
            return;
        }
        if (!isValidFullName(newFullName)) {
            showError("Tên không hợp lệ. Vui lòng kiểm tra lại.");
            return;
        }
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            dos.writeUTF("CHNAME " + newFullName);
            String Response = dis.readUTF();
            System.out.println(Response);
            if (Response.equals("OK")) {
                showSuccessAlert("Đã Lưu Cập Nhật. Đăng nhập lại để xem thay đổi.");
                // Close the login window when switching to the main screen
                Stage edit1Stage = (Stage) btnSave1.getScene().getWindow();
                edit1Stage.close();

            }else {
                showError("Kiểm tra lại thông tin nhập vào.");
            }

        } catch (IOException e) {
            showError("Lỗi khi giao tiếp với máy chủ: " + e.getMessage());
        }


    }

    private boolean isValidFullName(String fullName) {
        return fullName.matches("^[\\p{L}]+(\\s[\\p{L}]+)*$");

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
