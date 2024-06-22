package com.vku.email.test;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class clientTest {
    public static void main(String []args) throws IOException {
        Scanner sc = new Scanner(System.in);
        Socket socket = new Socket("localhost", 3010);
        OutputStream os = socket.getOutputStream();
        DataInputStream dis = new DataInputStream(socket.getInputStream());
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        String attachment = "1:2";
        while (true) {
            for (int i=0; i<attachment.split(":").length; i++) {
                dos.writeUTF(attachment.split(":")[i]+".txt");
                File file = new File("D:\\" + attachment.split(":")[i] + ".txt");
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                dis.readUTF();
                fis.close();
            }
        }

//        String email = "kien";
//        Socket socket1 = new Socket("localhost", 25);
//        DataOutputStream dos1 = new DataOutputStream(socket1.getOutputStream());
//        dos1.writeUTF(email);
//        Socket socket2 = new Socket("localhost", 21);
//        Scanner sc = new Scanner(System.in);
//        DataOutputStream dos2 = new DataOutputStream(socket2.getOutputStream());
//        dos2.writeUTF(sc.nextLine());
    }
}
