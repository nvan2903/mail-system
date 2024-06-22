package com.vku.email;

import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FTPExecute extends Thread{
    private Socket socket;
    private TextArea screen;
    private final Properties props = new Properties();
    private Connection connect;
    private Map<String, String> mailMap;
    private Text status;
    private String auth = "";
    private String receiver = "";
    public FTPExecute(Socket socket, Connection connect, TextArea screen, Map<String, String> mailMap, Text status) throws IOException {
        this.socket = socket;
        this.screen = screen;
        this.connect = connect;
        this.mailMap = mailMap;
        this.status = status;
        updateStatus(1);
        props.load(FTPExecute.class.getClassLoader().getResourceAsStream("application.properties"));
    }

    private int count = 2;
    private void updateStatus(int n) {
        if (count == 0) return;

        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(status.getText());
        if (m.find()) {
            int running = Integer.parseInt(m.group())+n;
            status.setText("FTP server ("+running+" running)");
        }

        count--;
    }

    @Override
    public void run() {
        DataInputStream dis;
        DataOutputStream dos;
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            String sms = dis.readUTF();
            if (sms.split(" ")[0].equals("FTP") && sms.split(" ").length > 1) {
                auth = sms.split(" ", 3)[1];
                if (sms.split(" ").length == 3) {
                    receiver = sms.split(" ", 3)[2];
                    if (receiver.equals(auth)) {
                        dos.writeUTF("NO");
                        socket.close();
                        updateStatus(-1);
                    }
                }
                dos.writeUTF("OK");
                startSession(dis, dos);
            } else {
                dos.writeUTF("NO");
                socket.close();
                updateStatus(-1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
                updateStatus(-1);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void startSession(DataInputStream dis, DataOutputStream dos) throws IOException {
        try {
            while (true) {
                String sms = dis.readUTF();
                String request = sms.split(" ")[0];
                switch(request) {
                    case "PUT" -> {
                        //timeout
                        AtomicInteger timeout = new AtomicInteger(10);
                        Thread t = new Thread(() -> {
                            try {
                            timeout.getAndDecrement();
                            if (timeout.get() <= 0) {
                                socket.close();
                                updateStatus(-1);
                            }
                                Thread.sleep(1000);
                            } catch (InterruptedException | IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        t.start();

                        File senderDir = new File(props.getProperty("userDir")+removeDomain(auth)+"\\"+mailMap.get(auth));
                        File receiverDir = new File(props.getProperty("userDir")+removeDomain(receiver)+"\\"+mailMap.get(auth));
                        if (!senderDir.exists() || !receiverDir.exists()) {
                            dos.writeUTF("NO");
                            socket.close();
                            updateStatus(-1);
                            return;
                        }
                        senderDir = new File(props.getProperty("userDir")+removeDomain(auth)+"\\"+mailMap.get(auth)+"\\"+"attachment");
                        receiverDir = new File(props.getProperty("userDir")+removeDomain(receiver)+"\\"+mailMap.get(auth)+"\\"+"attachment");
                        if (!senderDir.exists() && !receiverDir.exists()) {
                            senderDir.mkdir();
                            receiverDir.mkdir();
                        }
                        String filename = sms.split(" ", 2)[1];
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        try {
                            while ((bytesRead = socket.getInputStream().read(buffer)) != -1) {
                                FileOutputStream fosSender = new FileOutputStream(props.getProperty("userDir") +
                                        removeDomain(auth) + "\\" + mailMap.get(auth) + "\\attachment\\" + filename);
                                FileOutputStream fosReceiver = new FileOutputStream(props.getProperty("userDir") +
                                        removeDomain(receiver) + "\\" + mailMap.get(auth) + "\\attachment\\" + filename);
                                fosSender.write(buffer, 0, bytesRead);
                                fosReceiver.write(buffer, 0, bytesRead);
                                dos.writeUTF("OK");
                                fosSender.close();
                                fosReceiver.close();
                                break;
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            dos.writeUTF("NO");
                        }
                    }
                    case "RECV" -> {
                        if (sms.split(" ").length < 3) {
                            dos.writeUTF("NO");
                            break;
                        }
                        String dirId = getDirId(sms.split(" ")[1]);
                        String filename = sms.split(" ", 3)[2];
                        File file = new File(props.getProperty("userDir") +
                                removeDomain(auth) + "\\" + dirId + "\\attachment\\" + filename);
                        if (!file.exists()) {
                            dos.writeUTF("NO");
                            break;
                        }
                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        dos.writeUTF("OK");
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            socket.getOutputStream().write(buffer, 0, bytesRead);
                            break;
                        }
                    }
                    case "FORWARD" -> {
                        String mail_id = sms.split(" ")[1];
                        if (forward(mail_id)) {
                            dos.writeUTF("OK");
                        } else {
                            dos.writeUTF("NO");
                        }
                    }
                    case "QUIT" -> {
                        dos.writeUTF("OK");
                        socket.close();
                        updateStatus(-1);
                        mailMap.remove(auth);
                        return;
                    }
                    default -> dos.writeUTF("NO");
                }
                screen.appendText(new Date()+"\n");
                screen.appendText("<"+auth+"> request " + request + "\n");
                screen.appendText("------------------------------\n");
            }
        } catch (IOException | SQLException | ParseException e) {
            e.printStackTrace();
            try {
                dos.writeUTF("NO");
                socket.close();
                updateStatus(-1);
            } catch (IOException e2) {
                socket.close();
                updateStatus(-1);
                e2.printStackTrace();
            }
        }
    }

    private boolean forward(String mailId) throws SQLException, IOException {
        String content = "";
        String attachment = "";
        String sql = "SELECT content, attachment FROM mail WHERE id='"+mailId+"' AND owner='"+auth+"'";
        PreparedStatement stm = connect.prepareStatement(sql);
        ResultSet rs = stm.executeQuery();
        if (rs.next()) {
            content = rs.getString(1);
            attachment = rs.getString(2);
            if (attachment != null && !attachment.isEmpty()) {
                File file = new File(content);
                String sourceDir = file.getParent()+"\\attachment";

                String targetDir1 = props.getProperty("userDir")+removeDomain(auth)+"\\"+mailMap.get(auth)+"\\attachment";
                String targetDir2 = props.getProperty("userDir")+removeDomain(receiver)+"\\"+mailMap.get(auth)+"\\attachment";

                new File(targetDir1).mkdir();
                new File(targetDir2).mkdir();

                DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(sourceDir));
                for (Path entry : stream) {
                    Path newTarget = Paths.get(targetDir1).resolve(entry.getFileName());
                    Files.copy(entry, newTarget, StandardCopyOption.REPLACE_EXISTING);
                    newTarget = Paths.get(targetDir2).resolve(entry.getFileName());
                    Files.copy(entry, newTarget, StandardCopyOption.REPLACE_EXISTING);
                }
                mailMap.remove(auth);
            }
            return true;
        }
        return false;
    }

    private String getDirId(String mailId) throws SQLException, ParseException {
        String created_at = "";
        String sql = "SELECT created_at FROM mail WHERE id='"+mailId+"'";
        PreparedStatement stm = connect.prepareStatement(sql);
        ResultSet rs = stm.executeQuery();
        if (rs.next()) {
            created_at = rs.getString(1);
        }
        if (created_at.isEmpty()) created_at = "NOT_FOUND";
        else {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(created_at);
            created_at = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
        }
        return created_at;
    }
    private String removeDomain(String email) {
        int index = email.indexOf(props.getProperty("defaultDomain"));
        if (index == -1) return "";
        return email.substring(0, index);
    }
}
