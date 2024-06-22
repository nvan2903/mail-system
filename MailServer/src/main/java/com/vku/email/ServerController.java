package com.vku.email;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ServerController {
    private ServerSocket serverSMTP, serverIMAP, serverFTP;
    private final Map<String, String> mailMap = new HashMap<>();
    @FXML
    private Button startBtn;
    @FXML
    private TextArea screenSMTP, screenIMAP, screenFTP;

    @FXML
    private Text smtpText, imapText, ftpText;

    @FXML
    public void startServer() {
        try {
            Connection connect;
            try {
                connect = new ConnectDB().getConnection();
            } catch (SQLException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("");
                alert.setContentText("Can't connect to database");
                alert.showAndWait();
                return;
            }
            serverSMTP = new ServerSocket(25);
            serverIMAP = new ServerSocket(143);
            serverFTP =  new ServerSocket(21);
            Thread readSMTP = new Thread(() -> {
                while(true) {
                    try {
                        Socket socket = serverSMTP.accept();
                        SMTPExecute smtpExecute = new SMTPExecute(socket, connect, screenSMTP, mailMap, smtpText);
                        smtpExecute.start();
                    } catch (IOException e) {
                        break;
                    }
                }
            });
            Thread readIMAP = new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = serverIMAP.accept();
                        IMAPExecute imapExecute = new IMAPExecute(socket, connect, screenIMAP, imapText);
                        imapExecute.start();
                    } catch (IOException e) {
                        break;
                    }
                }
            });
            Thread readFTP = new Thread(() ->  {
               while (true) {
                   try {
                       Socket socket = serverFTP.accept();
                       FTPExecute ftpExecute = new FTPExecute(socket, connect, screenFTP, mailMap, ftpText);
                       ftpExecute.start();
                   } catch (IOException e) {
                       break;
                   }
               }
            });
            readIMAP.start();
            readSMTP.start();
            readFTP.start();
            startBtn.getScene().getWindow().setOnCloseRequest(windowEvent -> {
                try {
                    serverSMTP.close();
                    serverIMAP.close();
                    serverFTP.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            startBtn.setDisable(true);
            screenSMTP.appendText("Server SMTP is starting to listen...\n");
            screenIMAP.appendText("Server IMAP is starting to listen...\n");
            screenFTP.appendText("Server FTP is starting to listen...\n");
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("");
            alert.setContentText("Can't start server");
            alert.showAndWait();
        }
    }
}