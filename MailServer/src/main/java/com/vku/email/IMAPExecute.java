package com.vku.email;

import com.google.gson.Gson;
import com.vku.email.model.Account;
import com.vku.email.model.Mail;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class IMAPExecute extends Thread {
    private final Socket socket;
    private final Connection connect;
    private final TextArea screen;
    private final Gson gson = new Gson();
    private final Properties props = new Properties();
    private String auth = "";
    private final Text status;
    private int count = 2;

    public IMAPExecute(Socket socket, Connection connect, TextArea screen, Text status) throws IOException {
        this.socket = socket;
        this.connect = connect;
        this.screen = screen;
        this.status = status;
        updateStatus(1);
        props.load(IMAPExecute.class.getClassLoader().getResourceAsStream("application.properties"));
    }

    private void updateStatus(int n) {
        if (count == 0) return;

        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(status.getText());
        if (m.find()) {
            int running = Integer.parseInt(m.group())+n;
            status.setText("IMAP server ("+running+" running)");
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
            if (sms.equals("CAPABILITY")) {
                dos.writeUTF("OK");
                startSession(dis, dos);
            } else {
                dos.writeUTF("NO");
                socket.close();
                updateStatus(-1);
            }
        } catch (IOException e) {
            try {
                socket.close();
                updateStatus(-1);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    private void startSession(DataInputStream dis, DataOutputStream dos) throws IOException {
        while (true) {
            String sms = dis.readUTF();
            String request = sms.split(" ")[0];
            switch (request) {
                case "LOGIN" -> {
                    String email = sms.split(" ")[1];
                    if (email.endsWith(props.getProperty("defaultDomain"))) {
                        String pass = sms.split(" ")[2];
                        Account account = new Account();
                        if (login(account, email, pass)) {
                            dos.writeUTF("OK " +
                                    account.getEmail_address() + " " +
                                    account.getPassword() + " " +
                                    account.getFullname());
                            auth = email;
                        }
                        else {
                            dos.writeUTF("NO");
                        }
                    } else {
                        dos.writeUTF("NO");
                    }
                }
                case "REGISTER" -> {
                    String email = sms.split(" ")[1];
                    if (email.endsWith(props.getProperty("defaultDomain"))) {
                        File dir = new File(props.getProperty("userDir") + removeDomain(email));
                        if (!dir.exists()) {
                            String pass = sms.split(" ")[2];
                            String username = sms.split(" ", 4)[3];
                            Account account = new Account(email, pass, username);
                            if (register(email, pass, username)) {
                                if (dir.mkdir()) {
                                    dos.writeUTF("OK " +
                                            account.getEmail_address() + " " +
                                            account.getPassword() + " " +
                                            account.getFullname());
                                    auth = email;
                                } else dos.writeUTF("NO");
                            } else dos.writeUTF("NO");
                        } else dos.writeUTF("NO");
                    } else dos.writeUTF("NO");
                }
                default -> {
                    if (auth.isEmpty()) {
                        dos.writeUTF("NO");
                        continue;
                    }
                    switch (request) {
                        case "CHNAME" -> {
                            String fullname = sms.split(" ",2)[1];
                            if (changeName(fullname)) {
                                dos.writeUTF("OK");
                            } else {
                                dos.writeUTF("NO");
                            }
                        }
                        case "CHPASS" -> {
                            String password = sms.split(" ")[1];
                            String newPassword = sms.split(" ")[2];
                            if (changePass(password, newPassword)) {
                                dos.writeUTF("OK");
                            } else {
                                dos.writeUTF("NO");
                            }
                        }
                        case "SELECT" -> {
                            String mailBoxType = sms.split(" ")[1];
                            ArrayList<Mail> mailBox = new ArrayList<>();
                            if (getMailBox(mailBox, mailBoxType)) {
                                String json = gson.toJson(mailBox);
                                dos.writeUTF("OK " + json);
                            } else dos.writeUTF("NO");
                        }
                        case "FETCH" -> {
                            int mailId = Integer.parseInt(sms.split(" ")[1]);
                            Mail mail = new Mail();
                            mail.setId(mailId);
                            // TODO mail is replied
                            Mail replied = new Mail();
                            if (fetchMail(mail, replied) && getMailContent(mail, replied)) {
                                String json = gson.toJson(mail);
                                if (mail.getReply() == 0) dos.writeUTF("OK " + json);
                                else {
                                    String json2 = gson.toJson(replied);
                                    dos.writeUTF("OK " + json);
                                    dos.writeUTF("OK " + json2);
                                }
                            } else dos.writeUTF("NO");
                        }
                        case "DELETE" -> {
                            int mailId = Integer.parseInt(sms.split(" ")[1]);
                            if (deleteMail(mailId)) {
                                dos.writeUTF("OK");
                            } else dos.writeUTF("NO");
                        }
                        case "RESTORE" -> {
                            int mailId = Integer.parseInt(sms.split(" ")[1]);
                            if (restoreMail(mailId)) {
                                dos.writeUTF("OK");
                            } else dos.writeUTF("NO");
                        }
                        case "LOGOUT" -> {
                            dos.writeUTF("OK");
                            socket.close();
                            updateStatus(-1);
                            screen.appendText(new Date()+"\n");
                            screen.appendText("<" + auth + "> request " + request + "\n");
                            screen.appendText("------------------------------\n");
                            return;
                        }
                    }
                }
            }
            if (!auth.isEmpty()) {
                screen.appendText(new Date()+"\n");
                screen.appendText("<" + auth + "> request " + request + "\n");
                screen.appendText("------------------------------\n");
            }
        }
    }

    private boolean changeName(String fullname) {
        if (fullname == null || fullname.isEmpty()) return false;
        String sql = "UPDATE account SET fullname=? WHERE email_address=?";
        try {
            PreparedStatement stm = connect.prepareStatement(sql);
            stm.setString(1, fullname);
            stm.setString(2, auth);
            stm.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean changePass(String password, String newPassword) {
        if (password == null || password.isEmpty()) return false;
        if (newPassword == null || newPassword.isEmpty()) return false;
        String sql = "UPDATE account SET password=? WHERE email_address=? AND password=?";
        try {
            PreparedStatement stm = connect.prepareStatement(sql);
            stm.setString(1, Security.sha256(newPassword));
            stm.setString(2, auth);
            stm.setString(3, Security.sha256(password));
            stm.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean restoreMail(int mailId) {
        Mail mail = new Mail();
        mail.setId(mailId);
        if (!fetchMail(mail, new Mail())) return false;
        String sql = "UPDATE mail SET deleted_at=null WHERE id='" + mailId + "' AND owner='" + auth + "'";
        try {
            PreparedStatement stm = connect.prepareStatement(sql);
            stm.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean deleteMail(int mailId) {
        Mail mail = new Mail();
        mail.setId(mailId);
        if (!fetchMail(mail, new Mail())) return false;
        String sql;
        if (mail.getDeleted_at() != null) {
            sql = "DELETE FROM mail WHERE id='" + mailId + "' AND owner='" + auth + "'";
        } else {
            sql = "UPDATE mail SET deleted_at=current_timestamp WHERE id='" + mailId + "' AND owner='" + auth + "'";
        }
        try {
            PreparedStatement stm = connect.prepareStatement(sql);
            stm.execute();
            if (mail.getDeleted_at() != null) {
                File file = new File(mail.getContent());
                return file.delete();
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean fetchMail(Mail mail, Mail replied) {
        String sql = "SELECT created_at, sender, receiver, owner, is_read, attachment, subject, content, reply, deleted_at FROM mail WHERE id='"+ mail.getId() +"' AND owner='" + auth + "'";
        try {
            PreparedStatement stm = connect.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                mail.setCreated_at(rs.getString(1));
                mail.setSender(rs.getString(2));
                mail.setReceiver(rs.getString(3));
                mail.setOwner(rs.getString(4));
                mail.setIsRead(rs.getInt(5));
                mail.setAttachment(rs.getString(6));
                mail.setSubject(rs.getString(7));
                mail.setContent(rs.getString(8));
                mail.setReply(rs.getInt(9));
                mail.setDeleted_at(rs.getString(10));
                getFullname(mail);
                if (mail.getReply() != 0) {
                    sql = "SELECT id, created_at, sender, receiver, owner, is_read, attachment, subject, content, reply, deleted_at FROM mail WHERE id='"+ mail.getReply() +"'";
                    stm = connect.prepareStatement(sql);
                    rs = stm.executeQuery();
                    if (rs.next()) {
                        replied.setId(rs.getInt(1));
                        replied.setCreated_at(rs.getString(2));
                        replied.setSender(rs.getString(3));
                        replied.setReceiver(rs.getString(4));
                        replied.setOwner(rs.getString(5));
                        replied.setIsRead(rs.getInt(6));
                        replied.setAttachment(rs.getString(7));
                        replied.setSubject(rs.getString(8));
                        replied.setContent(rs.getString(9));
                        replied.setReply(rs.getInt(10));
                        replied.setDeleted_at(rs.getString(11));
                        getFullname(replied);
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void getFullname(Mail mail) {
        try {
            String sql = "SELECT fullname FROM account WHERE email_address='"+mail.getSender()+"'";
            PreparedStatement stm = connect.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                mail.setSenderName(rs.getString(1));
            }
            sql = "SELECT fullname FROM account WHERE email_address='"+mail.getReceiver()+"'";
            stm = connect.prepareStatement(sql);
            rs = stm.executeQuery();
            if (rs.next()) {
                mail.setReceiverName(rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean getMailContent(Mail mail, Mail replied) {
        try {
            File file = new File(mail.getContent());
            if (file.exists()) {
                FileReader fr = new FileReader(mail.getContent());
                BufferedReader br = new BufferedReader(fr);
                StringBuilder lines = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    lines.append(line).append("\n");
                }
                mail.setContent(lines.toString());
                if (mail.getIsRead() == 0) {
                    String sql = "UPDATE mail SET is_read=1 WHERE id='" + mail.getId() + "'";
                    PreparedStatement stm = connect.prepareStatement(sql);
                    stm.execute();
                }
            } else mail.setContent("");
            if (mail.getReply() != 0) {
                file = new File(replied.getContent());
                if (file.exists()) {
                    FileReader fr = new FileReader(replied.getContent());
                    BufferedReader br = new BufferedReader(fr);
                    String lines = "";
                    String line;
                    while ((line = br.readLine()) != null) {
                        lines = line + "\n";
                    }
                    replied.setContent(lines);
                } else replied.setContent("");
            }
                return true;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean getMailBox(ArrayList<Mail> mailBox, String mailBoxType) {
        String sql = "";
        switch (mailBoxType) {
            case "inbox" -> sql = "SELECT id, subject, owner, sender, receiver, is_read, created_at, reply FROM mail WHERE receiver='"+ auth +"' AND owner='" + auth +"' AND deleted_at IS NULL";
            case "outbox" -> sql = "SELECT id, subject, owner, sender, receiver, is_read, created_at, reply FROM mail WHERE sender='"+ auth +"' AND owner='" + auth +"' AND deleted_at IS NULL";
            case "recycle" -> sql = "SELECT id, subject, owner, sender, receiver, is_read, created_at, reply FROM mail WHERE owner='"+ auth +"' AND deleted_at IS NOT NULL";
        }
        try {
            PreparedStatement stm = connect.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Mail mail = new Mail();
                mail.setId(rs.getInt(1));
                mail.setSubject(rs.getString(2));
                mail.setOwner(rs.getString(3));
                mail.setSender(rs.getString(4));
                mail.setReceiver(rs.getString(5));
                mail.setIsRead(rs.getInt(6));
                mail.setCreated_at(rs.getString(7));
                mail.setReply(rs.getInt(8));
                mailBox.add(mail);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean register(String email, String pass, String fullname) {
        String sql = "INSERT INTO account VALUES(?,?,?, current_timestamp)";
        try {
            PreparedStatement stm = connect.prepareStatement(sql);
            stm.setString(1, email);
            stm.setString(2, fullname);
            stm.setString(3, Security.sha256(pass));
            stm.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean login(Account account, String email, String pass) {
        String sql = "SELECT * FROM account WHERE email_address=? AND password=?";
        try {
            PreparedStatement stm = connect.prepareStatement(sql);
            stm.setString(1, email);
            stm.setString(2, Security.sha256(pass));
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                account.setEmail_address(rs.getString(1));
                account.setFullname(rs.getString(2));
                account.setPassword(rs.getString(3));
            }
            else return false;
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String removeDomain(String email) {
        int index = email.indexOf(props.getProperty("defaultDomain"));
        if (index == -1) return "";
        return email.substring(0, index);
    }

}