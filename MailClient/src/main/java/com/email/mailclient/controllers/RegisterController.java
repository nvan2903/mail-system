package com.email.mailclient.controllers;

import com.email.mailclient.ConfigurationLoader;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.control.Alert.AlertType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

public class RegisterController {

    private Properties config;
    private Socket socket;
    private String loginResponse;

    public RegisterController() {
        config = new Properties();
        loadConfiguration(); // Tải cấu hình từ tệp
    }

    @FXML
    private Text txtLogin;
    @FXML
    private Button btnRegister;
    @FXML
    private TextField txtEmailAddress;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private PasswordField txtRepassword;

    @FXML
    private TextField txtUserName;



    @FXML
    void registerButtonClicked() throws IOException {
        String email = txtEmailAddress.getText();
        String password = txtPassword.getText();
        String rePassword = txtRepassword.getText();
        String username = txtUserName.getText();
        if (checkServerCapability()) {
            if (email.isEmpty() || password.isEmpty() || rePassword.isEmpty() || username.isEmpty()) {
                showAlert("Vui lòng điền đầy đủ thông tin.", AlertType.ERROR);
            } else if (!isValidEmail(email)) {
                showAlert("Định dạng email không hợp lệ hoặc tên miền không chính xác.", AlertType.ERROR);
            } else if (password.length() < 8) {
                showAlert("Mật khẩu phải có ít nhất 8 ký tự.", AlertType.ERROR);
            } else if (!password.equals(rePassword)) {
                showAlert("Mật khẩu không khớp.", AlertType.ERROR);
            } else if (isValidFullName(username)) {
                showAlert("Tên người dùng không hợp lệ", AlertType.ERROR);
            } else {
                // Gọi hàm gửi yêu cầu đăng ký
                if (sendRegisterRequest(email, password, username)) {
                    showSuccessAlert("Đăng ký thành công!");
                    switchToLogin();
                } else {
                    showAlert("Đăng ký không thành công. Vui lòng thử lại.", AlertType.ERROR);
                }
            }
        }
    }

    private boolean isValidEmail(String email) {
        // Kiểm tra định dạng email và tên miền
        String defaultDomain = "se.vku.vn";

        if (email.isEmpty()) {
            showAlert("Vui lòng nhập địa chỉ email.", AlertType.ERROR);
            return false;
        }

        // Nếu không có ký tự '@' trong email, thêm tên miền mặc định vào
        if (!email.contains("@")) {
            email = email + "@" + defaultDomain;
        }

        // Kiểm tra định dạng email và tên miền
        String emailRegex = "^[A-Za-z0-9+_.-]+@" + defaultDomain + "$";
        if (!email.matches(emailRegex)) {
            showAlert("Định dạng email không hợp lệ hoặc tên miền không chính xác.", AlertType.ERROR);
            return false;
        }

        return true;
    }



    private boolean isValidFullName(String username) {
        return username.matches("^[a-zA-Z\\\\p{L}]+(\\\\s[a-zA-Z\\\\p{L}]+)*$");

//        [a-zA-Z\\p{L}]:          Một ký tự thuộc bảng chữ cái tiếng Anh hoặc bất kỳ ký tự chữ cái nào thuộc bảng mã Unicode (được đại diện bởi \\p{L}).
//        +:                       Ít nhất một ký tự chữ cái hoặc ký tự chữ cái Unicode.
//        (\\s[a-zA-Z\\p{L}]+)*:   Một nhóm con có thể lặp lại, bao gồm khoảng trắng (\\s) theo sau là một hoặc nhiều ký tự chữ cái hoặc ký tự chữ cái Unicode.
//        $:                       Kết thúc của chuỗi.
    }


    private void showAlert(String message, AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean sendRegisterRequest(String email, String password, String username) {


        try {
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        DataInputStream dis = new DataInputStream(socket.getInputStream());

        dos.writeUTF("REGISTER " + email + config.getProperty("default.domain") + " " + password+ " "+ username);
        String registerResponse = dis.readUTF();
        System.out.println(registerResponse);
        if (registerResponse.startsWith("OK")) {
            return true;
        } else {
            return false;
        }
    } catch (IOException e) {
        showError("Lỗi kết nối máy chủ: " + e.getMessage());
    }

        return false;
    }

    @FXML
    void switchToLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/email/mailclient/login.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = (Stage) txtLogin.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.show();
    }



    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadConfiguration() {
        String configFileLocation = "config/mailclient.properties"; // Đường dẫn tới tệp cấu hình
        try (FileReader reader = new FileReader(configFileLocation)) {
            config.load(reader);
        } catch (IOException e) {
            System.err.println("Lỗi khi tải tệp cấu hình: " + e.getMessage());
            // Bạn có thể xử lý lỗi này theo cách cần thiết cho ứng dụng của bạn.
        }
    }

    private boolean checkServerCapability() {
        System.out.println("Đã load tệp cấu hình  "+ config);
        String serverIP = config.getProperty("server.ip");
        int serverPort = Integer.parseInt(config.getProperty("imap.port"));

        try {
            socket = new Socket(serverIP, serverPort);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            dos.writeUTF("CAPABILITY");
            String capabilityResponse = dis.readUTF();

            if (capabilityResponse.equals("OK")) {
                System.out.println("Máy chủ đã sẵn sàng: " + capabilityResponse);
                return true;
            } else {
                System.out.println("Máy chủ chưa sẵn sàng (NOT CAPABILITY): " + capabilityResponse);
                return false;
            }
        } catch (IOException e) {
            System.err.println("Lỗi kết nối đến máy chủ: " + e.getMessage());
            return false;
        }
    }

}
