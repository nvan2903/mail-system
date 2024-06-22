package com.vku.email.test;

import java.io.*;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Client {
    static DataInputStream dis;
    static DataOutputStream dos;
    public static void main(String[] args) throws IOException, ParseException {
        String serverIP = "localhost";


        // TODO login, work with mail
//        Socket socketImap = new Socket(serverIP,143); // 25: SMTP, 143: IMAP
//        dos = new DataOutputStream(socketImap.getOutputStream());
//        dis = new DataInputStream(socketImap.getInputStream());
//        String email = "vantn.21it@se.vku.vn";
//        String password = "12345678";
//        String fullname = "Nguyen Duc Trung";
//        communicate("CAPABILITY");
////        communicate("REGISTER " + email + " " + password + " " + fullname);
//        communicate("LOGIN " + email + " " + password);
//        communicate("SELECT outbox");
////        communicate("DELETE 110");
//        communicate("SELECT outbox");
//        communicate("SELECT inbox");
//        communicate("SELECT recycle");
////        communicate("RESTORE 110");
//        communicate("SELECT recycle");
//        communicate("FETCH 124");
//        communicate("LOGOUT");
//        socketImap.close();


        // TODO get file from mail
//        Socket socketFTP = new Socket(serverIP, 21);
//        DataOutputStream dos2 = new DataOutputStream(socketFTP.getOutputStream());
//        DataInputStream dis2 = new DataInputStream(socketFTP.getInputStream());
//        dos2.writeUTF("FTP ndtrung@se.vku.vn");
//        if (dis2.readUTF().equals("OK")) {
//            String mailId = "34";
//            String filename = "1.txt";
//            dos2.writeUTF("RECV " + mailId + " " + filename);
//            String resp = dis2.readUTF();
//            System.out.println("RECV " + resp);
//            if (resp.equals("OK")) {
//                InputStream is = socketFTP.getInputStream();
//                byte[] buffer = new byte[1024];
//                int bytesRead;
//                try {
//                    while ((bytesRead = is.read(buffer)) != -1) {
//                        FileOutputStream fos = new FileOutputStream("D:\\contain_file_receive\\" + filename);
//                        fos.write(buffer, 0, bytesRead);
//                        fos.close();
//                        break;
//                    }
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                    dos.writeUTF("NO");
//                }
//            }
//            dos2.writeUTF("QUIT");
//        }


        // TODO send email from ndtrung@se.vku.vn to tnvan@se.vku.vn with attachment
        // TODO send mail from client to server
        Socket socketSMTP = new Socket(serverIP,25);
        dos = new DataOutputStream(socketSMTP.getOutputStream());
        dis = new DataInputStream(socketSMTP.getInputStream());
        String sender = "trungnd.21it@se.vku.vn";
        String receiver = "vantn.21it@se.vku.vn";
        String subject = "Gửi file";
        String content = ":))";
        String attachment = "1.txt";
        communicate("HELO");
        communicate("MAIL FROM:"+sender);
        communicate("RCPT TO:"+receiver);
        communicate("DATA");
        communicate("SUBJECT:"+subject);
        communicate("CONTENT:"+content);
        communicate("ATTACH:"+attachment);
        dos.writeUTF("QUIT");
//        // TODO send file from client to server
        if (dis.readUTF().equals("OK")) {
            Socket socketFTP = new Socket("localhost", 21);
            DataOutputStream dos2 = new DataOutputStream(socketFTP.getOutputStream());
            DataInputStream dis2 = new DataInputStream(socketFTP.getInputStream());
            dos2.writeUTF("FTP " + sender + " " + receiver);
            String t = dis2.readUTF();
            System.out.println("server FTP said: " + t);
            if (t.equals("OK")) {
                OutputStream os = socketFTP.getOutputStream();
                for (int i=0; i<attachment.split(":").length; i++) {
                    File file = new File("D:\\" + attachment.split(":")[i]);
                    dos2.writeUTF("PUT " + attachment.split(":")[i]);
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        break;
                    }
                    String resp = dis2.readUTF();
                    System.out.println("send file " + attachment.split(":")[i] + " " + resp);
                    if (!resp.equals("OK")) break;
                }
                // end session
                dos2.writeUTF("QUIT");
                System.out.println("QUIT FTP " + dis2.readUTF());
            }
        }

        // TODO reply mail
//        Socket socketSMTP = new Socket(serverIP,25);
//        dos = new DataOutputStream(socketSMTP.getOutputStream());
//        dis = new DataInputStream(socketSMTP.getInputStream());
//        String sender = "tnvan@se.vku.vn";
//        String receiver = "ndtrung@se.vku.vn";
//        String reply = "97";
//        String subject = "xin chao";
//        String content = "Chao trung\nMinh la van day";
//        communicate("HELO");
//        communicate("MAIL FROM:"+sender);
//        communicate("RCPT TO:"+receiver);
//        communicate("REPLY:"+reply);
//        communicate("DATA");
//        communicate("SUBJECT:"+subject);
//        communicate("CONTENT:"+content);
//        dos.writeUTF("QUIT");

        // TODO forward mail
//        Socket socketSMTP = new Socket(serverIP,25);
//        dos = new DataOutputStream(socketSMTP.getOutputStream());
//        dis = new DataInputStream(socketSMTP.getInputStream());
//        String forward_from = "vantn.21it@se.vku.vn";
//        String forward_to = "tuanvd.21it@se.vku.vn";
//        String mail_id = "123";
//        communicate("HELO");
//        communicate("FORWARD FROM:"+forward_from);
//        communicate("FORWARD TO:"+forward_to);
//        communicate("MAIL FORWARD:"+mail_id);
//        dos.writeUTF("QUIT");
//        if (dis.readUTF().equals("OK")) {
//            Socket socketFTP = new Socket("localhost", 21);
//            DataOutputStream dos2 = new DataOutputStream(socketFTP.getOutputStream());
//            DataInputStream dis2 = new DataInputStream(socketFTP.getInputStream());
//            dos2.writeUTF("FTP " + forward_from + " " + forward_to);
//            if (dis2.readUTF().equals("OK")) {
//                dos2.writeUTF("FORWARD "+mail_id);
//                // end session
//                dos2.writeUTF("QUIT");
//                System.out.println("QUIT FTP " + dis2.readUTF());
//            }
//        }
    }

    public static void communicate(String request) throws IOException {
        dos.writeUTF(request);
        System.out.println("client said: " + request);
        System.out.println("client said: " + request + " - server said: " + dis.readUTF());
    }

}
