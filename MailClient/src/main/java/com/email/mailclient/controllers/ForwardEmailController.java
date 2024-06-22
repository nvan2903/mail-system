package com.email.mailclient.controllers;

import com.email.mailclient.model.EmailMessage;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

public class ForwardEmailController implements Initializable {
    private Properties config;
    static DataInputStream dis;
    static DataOutputStream dos;

    private String forwardFrom = "";
    private String ftpCommand = "";
    private List<File> selectedFiles;

    private String forwardTo = "";

    private EmailMessage forwardMessage;


    private File attachedFile;
    @FXML
    private VBox attachmentArea;


    @FXML
    private TextField txtRecipient, txtSubject;
    @FXML
    private HTMLEditor txtContent;
    @FXML
    private Button btnAttachFile;

    public ForwardEmailController(Properties config, String personalEmail, EmailMessage forwardMessage) {

        this.config = config;
        this.forwardFrom = personalEmail;
        this.forwardMessage = forwardMessage;
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Khởi tạo các thiết lập ban đầu nếu cần
        txtSubject.setText(forwardMessage.getSubject());
        txtSubject.setEditable(false);
        txtContent.setHtmlText(forwardMessage.getContent());
        txtContent.setDisable(true);
    }

//    @FXML
//    private void btnAttachFileAction() {
//        FileChooser fileChooser = new FileChooser();
//        fileChooser.setTitle("Chọn Tệp Đính Kèm");
//
//        // Cho phép chọn nhiều tệp
//        selectedFiles = fileChooser.showOpenMultipleDialog(null);
//
//        if (selectedFiles != null && !selectedFiles.isEmpty()) {
//            // Lưu danh sách tệp đã chọn
//            for (File file : selectedFiles) {
//                System.out.println("Đã chọn tệp đính kèm: " + file.getAbsolutePath());
//
//                // Tạo Label cho tệp và thêm vào HBox
//                Label fileLabel = new Label(file.getName());
//                fileLabel.getStyleClass().add("attached-file-label"); // (Optional) Thêm kiểu CSS nếu muốn
//                attachmentArea.getChildren().add(fileLabel);
//            }
//            System.out.println(selectedFiles);
//
//            ftpCommand = listToColonSeparatedString(selectedFiles);
//
//
//
//        }
//    }


    @FXML
    private void btnSendAction() throws IOException {
        forwardTo = txtRecipient.getText() + config.getProperty("default.domain");
        String serverIP = config.getProperty("server.ip");
        int serverPort = Integer.parseInt(config.getProperty("smtp.port"));

        Socket socket = new Socket(serverIP, serverPort);
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());

        communicate("HELO");
        communicate("FORWARD FROM:" + forwardFrom);
        communicate("FORWARD TO:" + forwardTo);
        communicate("MAIL FORWARD:" + forwardMessage.getId());
        dos.writeUTF("QUIT");
        if (dis.readUTF().equals("OK")) {
            Socket socketFTP = new Socket(config.getProperty("server.ip"), 21);
            DataOutputStream dos2 = new DataOutputStream(socketFTP.getOutputStream());
            DataInputStream dis2 = new DataInputStream(socketFTP.getInputStream());
            dos2.writeUTF("FTP " + forwardFrom + " " + forwardTo);
            if (dis2.readUTF().equals("OK")) {
                OutputStream os = socketFTP.getOutputStream();
                dos2.writeUTF("FORWARD " + forwardMessage.getId());
                // end session
                dos2.writeUTF("QUIT");
                System.out.println("QUIT FTP " + dis2.readUTF());
            }
        }


        txtRecipient.setText("");
        showSuccessAlert("Đã gửi");



    }

    public static void communicate(String request) throws IOException {
        dos.writeUTF(request);
        System.out.println("client said: " + request);
        System.out.println("client said: " + request + " - server said: " + dis.readUTF());
    }


    private void showSuccessAlert(String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String listToColonSeparatedString(List<File> fileList) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < fileList.size(); i++) {
            result.append(fileList.get(i).getName()); // Lấy tên file thay vì đường dẫn

            if (i < fileList.size() - 1) {
                result.append(":");
            }
        }

        return result.toString();
    }
}
