package com.email.mailclient.controllers;

import com.email.mailclient.model.EmailMessage;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EmailDetailsController extends CommonController implements Initializable {

    private static final String DOWNLOADS_PATH = System.getProperty("user.home") + "/Downloads/";
    static DataInputStream dis;
    static DataOutputStream dos;
    private Properties config;
    private Socket socket;
    private EmailMessage selectedEmail;
    private EmailMessage emailMessage;
    private String username="";
    private String emailAddress="";
    String dowloadLocation= "C:\\Users\\PC\\Downloads\\";
    @FXML
    private HBox attachmentHbox;

    @FXML
    private Label txtSubject;
    @FXML
    private Label txtSender;
    @FXML
    private Label txtRecipient;
    @FXML
    private Label txtDate;
    @FXML
    private WebView txtContent;
    @FXML
    private void loadReplyEmail(){

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/email/mailclient/reply-email.fxml"));
                loader.setControllerFactory(c -> new ReplyEmailController(config, emailAddress, emailMessage) );
                Parent root = loader.load();

                // Create a new stage
                Stage composeStage = new Stage();
                composeStage.initStyle(StageStyle.DECORATED);
                composeStage.initModality(Modality.APPLICATION_MODAL);
                composeStage.setTitle("Trả Lời");

                // Set the scene
                Scene scene = new Scene(root);
                composeStage.setScene(scene);

                // Show the stage
                composeStage.show();
            } catch (IOException e) {
                e.printStackTrace(); // Handle the exception appropriately
            }

    }
    @FXML
    private void loadForwardEmail(){


            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/email/mailclient/forward-email.fxml"));
                loader.setControllerFactory(c -> new ForwardEmailController(config, emailAddress, emailMessage));
                Parent root = loader.load();

                // Create a new stage
                Stage composeStage = new Stage();
                composeStage.initStyle(StageStyle.DECORATED);
                composeStage.initModality(Modality.APPLICATION_MODAL);
                composeStage.setTitle("Chuyển Tiếp");

                // Set the scene
                Scene scene = new Scene(root);
                composeStage.setScene(scene);

                // Show the stage
                composeStage.show();
            } catch (IOException e) {
                e.printStackTrace(); // Handle the exception appropriately
            }

        }



    public EmailDetailsController(Properties config, Socket socket, EmailMessage selectedEmail, String loginResponse) {
        super();
        this.config = config;
        this.socket = socket;
        this.selectedEmail = selectedEmail;

// thông tin đăng nhập
        String[] responseParts = loginResponse.split(" ", 4);
        String username = responseParts[3];
        String emailAddress = responseParts[1];
        this.username = username;
        this.emailAddress = emailAddress;


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


    private void fetchSelectedEmail() {
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            dos.writeUTF("FETCH " + selectedEmail.getId());
            String emailDetailResponse = dis.readUTF();
            String emailDetailReplied = "";
            if (selectedEmail.getReply() != 0) {
                emailDetailReplied = dis.readUTF();
            }

            String[] responseParts = emailDetailResponse.split(" ", 2);
            if (responseParts.length >= 2) {
                String emailDetailPart = responseParts[1];

                Gson gson = new Gson();
                emailMessage = gson.fromJson(emailDetailPart, EmailMessage.class);
                System.out.println(emailMessage.getSubject());
                txtSender.setText(emailMessage.getSenderName() + " <" + emailMessage.getSender() + ">");
                if (emailMessage.getReceiver().equals(emailAddress)) {
                    txtRecipient.setText("đến tôi");
                } else {
                    txtRecipient.setText("đến " + emailMessage.getReceiver());
                }

                txtSubject.setText(emailMessage.getSubject());
                txtDate.setText(emailMessage.getCreated_at());
                loadPlainTextContent(emailMessage.getContent());


                if (emailMessage.getAttachment() != null && !emailMessage.getAttachment().isEmpty()) {
                    // nếu trường attachment không null và không rỗng .
                    // ví dụ attachment="file1.txt:file2.txt:file3.txt"

                    // tách chuỗi mỗi phần ngăn cahs bởi dấu : tương ứng 1 file
                    String[] attachments = emailMessage.getAttachment().split(":");
                    System.out.println("ds tep dinh kem: "+attachments.length);
                    // tương ứng mỗi file tạo 1 label cho nó và hiển thị tên file
                    for (String attachment : attachments) {
                        // Assuming you want to create a label for each file and display its name
                        // Tạo Label cho tệp và thêm vào HBox
                        Label fileLabel = new Label(attachment);
                        fileLabel.getStyleClass().add("attached-file-label"); // (Optional) Thêm kiểu CSS nếu muốn
                        attachmentHbox.getChildren().add(fileLabel);

                        // Add event handler for mouse click
                        fileLabel.setOnMouseClicked(event -> {
                            // Handle the click event, for example, open the file or perform some action
                            System.out.println("Clicked on file: " + attachment);
                            try {
                                downloadFile(attachment);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                        });
                    }

                }


            } else {
                System.out.println("Có lỗi xảy ra");
            }
        } catch (IOException e) {
            showError("Lỗi khi giao tiếp với máy chủ: " + e.getMessage());
        }
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fetchSelectedEmail();
        txtContent.setDisable(true);
    }

    private void loadPlainTextContent(String content) {
        WebEngine webEngine = txtContent.getEngine();
        // Wrapping content in a simple HTML structure
        webEngine.loadContent(content);
    }





    private void downloadFile(String filename) throws IOException {
        Socket socketFTP = new Socket(config.getProperty("server.ip"), 21);
             DataOutputStream dos2 = new DataOutputStream(socketFTP.getOutputStream());
             DataInputStream dis2 = new DataInputStream(socketFTP.getInputStream());

        dos2.writeUTF("FTP "+ emailAddress);
        if (dis2.readUTF().equals("OK")) {
            dos2.writeUTF("RECV " + emailMessage.getId() + " " + filename);
            String response = dis2.readUTF();
            System.out.println("RECV " + emailMessage.getId() + " " + filename);
            System.out.println("RECV " + response);
            if (response.equals("OK")) {
                InputStream is = socketFTP.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                try {
                    while ((bytesRead = is.read(buffer)) != -1) {
                        FileOutputStream fos = new FileOutputStream(dowloadLocation + filename);
                        fos.write(buffer, 0, bytesRead);
                        fos.close();
                        break;
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    dos.writeUTF("NO");
                }
                showSuccessAlert("Tải tệp thành công! Xem tại Dowloads.");
            }

            dos2.writeUTF("QUIT");
        }else {
            showError("Tải tệp thất bại!");
        }


    }



}





