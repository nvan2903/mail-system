package com.email.mailclient.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

public class LoginController {
    private VBox mainContentVBox;  // Giả sử bạn có một VBox trong main.fxml

    private Properties config;
    private Socket socket;
    private String loginResponse,personalEmail;

    public LoginController() {
        config = new Properties();
        loadConfiguration(); // Tải cấu hình từ tệp
    }

    @FXML
    private TextField txtEmailAddress;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Text txtRegister;

    @FXML
    private Button btnLogin;

    @FXML
    void switchToRegister() throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/email/mailclient/register.fxml"));
            loader.setControllerFactory(controllerClass -> new RegisterController());
            Parent root = loader.load();

            // Tạo một Scene với giao diện đã tải
            Scene scene = new Scene(root);

            // Lấy Stage hiện tại
            Stage stage = (Stage) txtRegister.getScene().getWindow();

            // Đặt Scene mới cho Stage
            stage.setScene(scene);
            stage.setTitle("Register");
            stage.show();

        } catch (IOException e) {
            System.err.println("Lỗi khi tải giao diện đăng ký: " + e.getMessage());
        }
    }


    @FXML
    void loginButtonClicked() throws IOException {
        String email = txtEmailAddress.getText();
        this.personalEmail = email;
        String password = txtPassword.getText();

        if (checkServerCapability()) {
            if (email.isEmpty() || password.isEmpty()) {
                showError("Vui lòng điền đầy đủ thông tin.");
            } else if (!isValidEmail(email)) {
                showError("Định dạng email không hợp lệ hoặc không sử dụng tên miền mặc định.");
            } else if (password.length() < 8) {
                showError("Mật khẩu phải có ít nhất 8 ký tự.");
            } else {
                boolean loggedIn = authenticateUser(email, password);

                if (loggedIn) {
                    showSuccessAlert("Thành công!");
                    switchToMainScreen(config, socket, loginResponse);
                } else {
                    showError("Đăng nhập không thành công. Vui lòng kiểm tra tên người dùng và mật khẩu.");
                }
            }
        }
    }

    private boolean isValidEmail(String email) {
        String defaultDomain = "se.vku.vn";

        // Nếu không có ký tự '@' trong email, thêm tên miền mặc định vào
        if (!email.contains("@")) {
            email = email + "@" + defaultDomain;
        }

        // Định dạng email không hợp lệ nếu không khớp với biểu thức chính quy
        String emailRegex = "^[A-Za-z0-9+_.-]+@" + defaultDomain + "$";
        if (!email.matches(emailRegex)) {
            showError("Tên miền không hợp lệ hoặc không sử dụng tên miền mặc định.");
            return false;
        }

        return true;
    }



    private boolean authenticateUser(String email, String password) {



            try {
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 DataInputStream dis = new DataInputStream(socket.getInputStream());



                dos.writeUTF("LOGIN " + email + config.getProperty("default.domain") + " " + password);
                String loginResponse = dis.readUTF();
                System.out.println(loginResponse);
                this.loginResponse = loginResponse;
                if (loginResponse.startsWith("OK")) {
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                showError("Lỗi kết nối máy chủ: " + e.getMessage());
            }

        return false;
    }

    private void switchToMainScreen(Properties config, Socket socket, String loginResponse) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/email/mailclient/main.fxml"));
                MainController mainController = new MainController( config, socket, personalEmail, loginResponse);
                loader.setControllerFactory(controllerClass -> mainController);
                Parent root = loader.load();

                // Tạo một Scene với giao diện đã tải
                Scene scene = new Scene(root);

                // Lấy Stage hiện tại
                Stage stage = new Stage();  // Create a new stage for the main screen
                stage.setScene(scene);
                stage.setTitle("Dashboard");

                // Close the login window when switching to the main screen
                Stage loginStage = (Stage) btnLogin.getScene().getWindow();
                loginStage.close();

                // Set up any additional configuration for the main stage if needed

                // Show the main screen
                stage.show();

            } catch (IOException e) {
                showError("Lỗi khi tải giao diện trang chủ: " + e.getMessage());

            }
        });
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
