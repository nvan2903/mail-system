package com.vku.email;

import com.vku.email.model.Mail;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SMTPExecute extends Thread {
    private final Socket socket;
    private final Connection connect;
    private final TextArea screen;
    private final Text status;
    private final Properties props = new Properties();
    private final Map<String, String> mailMap;
    public SMTPExecute(Socket socket, Connection connect, TextArea screen, Map<String, String> mailMap, Text status) throws IOException {
        this.socket = socket;
        this.connect = connect;
        this.screen = screen;
        this.mailMap = mailMap;
        this.status = status;
        updateStatus(1);
        props.load(SMTPExecute.class.getClassLoader().getResourceAsStream("application.properties"));
    }
    private int count = 2;
    private void updateStatus(int n) {
        if (count == 0) return;

        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(status.getText());
        if (m.find()) {
            int running = Integer.parseInt(m.group())+n;
            status.setText("SMTP server ("+running+" running)");
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
            if (sms.equals("HELO")) {
                dos.writeUTF("OK");
                startSession(dis, dos);
            } else {
                dos.writeUTF("NO");
                socket.close();
                updateStatus(-1);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            try {
                socket.close();
                updateStatus(-1);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void startSession(DataInputStream dis, DataOutputStream dos) throws IOException, InterruptedException {
        boolean data = false;
        String sender = "";
        String receiver = "";
        int reply = 0;
        String forward_from = "";
        String forward_to = "";
        int forward_mail = 0;
        String subject = "";
        String content = "";
        String attachment = "";
        while (true) {
            String sms = dis.readUTF();
            String request = sms.split(":")[0];
            switch (request) {
                case "DATA" -> {
                    data = true;
                    dos.writeUTF("OK");
                }
                case "MAIL FROM" -> {
                    sender = sms.split(":", 2)[1];
                    File dir = new File(props.getProperty("userDir")+removeDomain(sender));
                    if (dir.exists()) {
                        dos.writeUTF("OK");
                    }
                    else {
                        dos.writeUTF("NO");
                        return;
                    }
                }
                case "RCPT TO" -> {
                    receiver = sms.split(":", 2)[1];
                    File dir = new File(props.getProperty("userDir")+removeDomain(receiver));
                    if (dir.exists()) dos.writeUTF("OK");
                    else {
                        dos.writeUTF("NO");
                        return;
                    }
                }
                case "FORWARD FROM" -> {
                    forward_from = sms.split(":", 2)[1];
                    File dir = new File(props.getProperty("userDir")+removeDomain(forward_from));
                    if (dir.exists()) dos.writeUTF("OK");
                    else {
                        dos.writeUTF("NO");
                        return;
                    }
                }
                case "FORWARD TO" -> {
                    forward_to = sms.split(":", 2)[1];
                    File dir = new File(props.getProperty("userDir")+removeDomain(forward_to));
                    if (dir.exists()) dos.writeUTF("OK");
                    else {
                        dos.writeUTF("NO");
                        return;
                    }
                }
                case "MAIL FORWARD" -> {
                    forward_mail = Integer.parseInt(sms.split(":", 2)[1]);
                    dos.writeUTF("OK");
                }
                case "REPLY" -> {
                    reply = Integer.parseInt(sms.split(":", 2)[1]);
                    dos.writeUTF("OK");
                }
                case "SUBJECT" -> {
                    if (data) {
                        subject = sms.split(":", 2)[1];
                        dos.writeUTF("OK");
                    } else dos.writeUTF("NO");
                }
                case "CONTENT" -> {
                    if (data) {
                        content = sms.split(":", 2)[1];
                        dos.writeUTF("OK");
                    } else dos.writeUTF("NO");
                }
                case "ATTACH" -> {
                    if (data) {
                        attachment = sms.split(":", 2)[1];
                        dos.writeUTF("OK");
                    } else dos.writeUTF("NO");
                }
                case "QUIT" -> {
                    if (forward_mail != 0) {
                        if (forward(forward_mail, forward_from, forward_to)) {
                            dos.writeUTF("OK");
                            screen.appendText(new Date()+"\n");
                            screen.appendText("<"+forward_from+"> forward email to <"+forward_to+">\n");
                            screen.appendText("------------------------------\n");
                        } else {
                            dos.writeUTF("NO");
                        }
                        socket.close();
                        updateStatus(-1);
                        return;
                    }
                    if (data) {
                        if (sendMail(sender, receiver, reply, subject, content, attachment)) {
                            dos.writeUTF("OK");
                            screen.appendText(new Date()+"\n");
                            screen.appendText("<"+sender+"> send email to <"+receiver+">\n");
                            screen.appendText("------------------------------\n");
                        } else dos.writeUTF("NO");
                        socket.close();
                        updateStatus(-1);
                    }
                    return;
                }
                default -> {
                    dos.writeUTF("NO");
                    socket.close();
                    updateStatus(-1);
                    return;
                }
            }
        }
    }

    private boolean forward(int mail_id, String forward_from, String forward_to) {
        if (forward_from.equals(forward_to)) return false;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        try {
            String created_at = "", sender = "", receiver = "", attachment = "", subject = "", content = "";

            String sql = "SELECT created_at, sender, receiver, attachment, subject, content FROM mail " +
                    "WHERE id='"+mail_id+"' AND owner='"+forward_from+"'";
            PreparedStatement stm = connect.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                created_at = rs.getString(1);
                sender = rs.getString(2);
                receiver = rs.getString(3);
                attachment = rs.getString(4);
                subject = rs.getString(5);
                content = rs.getString(6);

                FileReader fr = new FileReader(content);
                BufferedReader br = new BufferedReader(fr);
                String lines = "";
                String line;
                while ((line = br.readLine()) != null) {
                    lines = line + "\n";
                }
                content = lines;

                String forward_message = "<---------Forwarded message---------><br>"+
                        "From "+sender+" To "+receiver+"<br>"+
                        "Date: "+created_at+"<br>";
                forward_message = "<html><body>"+forward_message+"</body></html>\n";
                content = forward_message + content;

                if (!(new File(props.getProperty("userDir")+removeDomain(forward_from)+"\\"+timestamp)).mkdir()) return false;
                if (!(new File(props.getProperty("userDir")+removeDomain(forward_to)+"\\"+timestamp)).mkdir()) return false;
                File fileFrom = new File(props.getProperty("userDir")+removeDomain(forward_from)+"\\"+timestamp, timestamp+".txt");
                File fileTo = new File(props.getProperty("userDir")+removeDomain(forward_to)+"\\"+timestamp, timestamp+".txt");

                FileWriter writer = new FileWriter(fileFrom);
                writer.write(content);
                writer.close();
                writer = new FileWriter(fileTo);
                writer.write(content);
                writer.close();

                if (attachment != null && !attachment.isEmpty()) {
                    mailMap.put(forward_from, timestamp);
                }
                Date date = new SimpleDateFormat("yyyyMMdd_HHmmss").parse(timestamp);
                created_at = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);

                insertMail(new Mail(0,created_at, forward_from, forward_to, 0, forward_from, 1, attachment, subject, content, ""), timestamp);
                insertMail(new Mail(0,created_at, forward_from, forward_to, 0, forward_to, 0, attachment, subject, content, ""), timestamp);
                return true;
            }
            return false;
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private boolean sendMail(String sender, String receiver, int reply, String subject, String content, String attachment) {
        if (sender.equals(receiver)) return false;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        if (!(new File(props.getProperty("userDir")+removeDomain(sender)+"\\"+timestamp)).mkdir()) return false;
        if (!(new File(props.getProperty("userDir")+removeDomain(receiver)+"\\"+timestamp)).mkdir()) return false;
        File fileSender = new File(props.getProperty("userDir")+removeDomain(sender)+"\\"+timestamp, timestamp+".txt");
        File fileReceiver = new File(props.getProperty("userDir")+removeDomain(receiver)+"\\"+timestamp, timestamp+".txt");
        try {
            FileWriter writer = new FileWriter(fileSender);
            writer.write(content);
            writer.close();
            writer = new FileWriter(fileReceiver);
            writer.write(content);
            writer.close();

            if (attachment != null && !attachment.isEmpty()) {
                mailMap.put(sender, timestamp);
            }
            Date date = new SimpleDateFormat("yyyyMMdd_HHmmss").parse(timestamp);
            String created_at = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);

            insertMail(new Mail(0, created_at, sender, receiver, reply, sender, 1, attachment, subject, content, ""), timestamp);
            insertMail(new Mail(0, created_at, sender, receiver, reply, receiver, 0, attachment, subject, content, ""), timestamp);
            return true;
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private void insertMail(Mail mail, String timestamp) throws SQLException {
        String sql = "INSERT INTO " +
                "mail (id, created_at, sender, receiver, owner, is_read, attachment, subject, content, reply, deleted_at)" +
                "VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL);";
        PreparedStatement stm = connect.prepareStatement(sql);
        stm.setString(1, mail.getCreated_at());
        stm.setString(2, mail.getSender());
        stm.setString(3, mail.getReceiver());
        stm.setString(4, mail.getOwner());
        stm.setInt(5, mail.getIsRead());
        if (mail.getAttachment() == null || mail.getAttachment().isEmpty()) stm.setNull(6, Types.VARCHAR);
        else stm.setString(6, mail.getAttachment());
        if (mail.getSubject() == null || mail.getSubject().isEmpty()) stm.setNull(7, Types.VARCHAR);
        else stm.setString(7, mail.getSubject());
        stm.setString(8, props.getProperty("userDir")+removeDomain(mail.getOwner())+"\\"+timestamp+"\\"+timestamp+".txt");
        if (mail.getReply() == 0) stm.setNull(9, Types.VARCHAR);
        else stm.setInt(9, mail.getReply());
        stm.execute();
    }

    private String removeDomain(String email) {
        int index = email.indexOf(props.getProperty("defaultDomain"));
        if (index == -1) return "";
        return email.substring(0, index);
    }
}