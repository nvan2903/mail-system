package com.email.mailclient.controllers;

import com.email.mailclient.model.EmailMessage;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

public class ReplyEmailController implements Initializable {
    private Properties config;
    static DataInputStream dis;
    static DataOutputStream dos;

    private String personalEmail = "";
    List<File> selectedFiles;
    String ftpCommand = "";

    String sender = "";
    String recipient = "";
    String subject = "";
    String content = "";
    int replyId ;

    private File attachedFile;
    @FXML
    private VBox attachmentArea;


    @FXML
    private TextField txtRecipient, txtSubject;
    @FXML
    private HTMLEditor txtContent;
    @FXML
    private Button btnAttachFile;

    public ReplyEmailController(Properties config, String personalEmail, EmailMessage emailFromDetail) {

        this.config = config;
        this.personalEmail = personalEmail;
        this.recipient = emailFromDetail.getSender();
        this.replyId = emailFromDetail.getId();
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtRecipient.setText(recipient);
        txtRecipient.setEditable(false);
    }

    @FXML
    private void btnAttachFileAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn Tệp Đính Kèm");

        // Cho phép chọn nhiều tệp
        selectedFiles = fileChooser.showOpenMultipleDialog(null);

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            // Lưu danh sách tệp đã chọn
            for (File file : selectedFiles) {
                // Check if the file size is less than 1KB
                if (file.length() < 1024) {
                    System.out.println("Đã chọn tệp đính kèm: " + file.getAbsolutePath());

                    // Tạo Label cho tệp và thêm vào HBox
                    Label fileLabel = new Label(file.getName());
                    fileLabel.getStyleClass().add("attached-file-label"); // (Optional) Thêm kiểu CSS nếu muốn
                    attachmentArea.getChildren().add(fileLabel);
                } else {
                    // Show an alert for files larger than 1KB
                    Alert alert = new Alert(Alert.AlertType.WARNING,
                            "File '" + file.getName() + "' is larger than 1KB and won't be attached.",
                            ButtonType.OK);
                    alert.showAndWait();
                }
            }
            System.out.println(selectedFiles);

            ftpCommand = listToColonSeparatedString(selectedFiles);



        }
    }


    @FXML
    private void btnSendAction() throws IOException {
        sender = personalEmail;
        subject = txtSubject.getText();
        content = txtContent.getHtmlText();


        String serverIP = config.getProperty("server.ip");
        int smtpPort = Integer.parseInt(config.getProperty("smtp.port"));

        Socket socketSMTP = new Socket(serverIP, smtpPort);
        dos = new DataOutputStream(socketSMTP.getOutputStream());
        dis = new DataInputStream(socketSMTP.getInputStream());
        communicate("HELO");
        communicate("MAIL FROM:" + sender);
        communicate("RCPT TO:" + recipient);
        communicate("REPLY:" + replyId);
        communicate("DATA");
        communicate("SUBJECT:" + subject);
        communicate("CONTENT:" + content);
        communicate("ATTACH:"+ftpCommand);
        dos.writeUTF("QUIT");
        String resp = dis.readUTF();
        System.out.println("send content ok = " + resp);
        if (resp.equals("OK") && !ftpCommand.isEmpty()) {

            Socket socketFTP = new Socket(config.getProperty("server.ip"), Integer.parseInt(config.getProperty("ftp.port")));
            DataOutputStream dos2 = new DataOutputStream(socketFTP.getOutputStream());
            DataInputStream dis2 = new DataInputStream(socketFTP.getInputStream());
            dos2.writeUTF("FTP " + sender + " " + recipient);
            if (dis2.readUTF().equals("OK")) {
                OutputStream os = socketFTP.getOutputStream();
                for (File file : selectedFiles) {

                    dos2.writeUTF("PUT " + file.getName());
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        break;
                    }
                    String response = dis2.readUTF();
                    System.out.println(resp);
                    if (!response.equals("OK")) break;
                }
                // end session
                dos2.writeUTF("QUIT");
                System.out.println("QUIT FTP " + dis2.readUTF());
            }
        }

        txtSubject.setText("");
        txtContent.setHtmlText("");
        showSuccessAlert("Đã gửi");

//        }


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
