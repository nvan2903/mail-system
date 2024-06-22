package com.vku.email.test;


import java.io.*;
import java.net.ServerSocket;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test {
    public static void main(String[] args) throws IOException, ParseException {

//        ServerSocket server1 = new ServerSocket(3010);
//        Socket socket = server1.accept();

//        Object lock = new Object();

//        t = new Thread(() -> {
//            synchronized (lock) {
//                for (int i=0;i<5; i++) {
//                    System.out.println("t1 " + i);
//                    if (i==3) {
//                        try {
//                            lock.wait();
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                }
//            }
//        });
//        t2 = new Thread(() -> {
//            synchronized (lock) {
//                for (int i=0; i<10; i++) {
//                    System.out.println("t2 " + i);
//                }
//                lock.notify();
//            }
//        });
//        t.start();
//        t2.start();
//        try {
//            t2.join();
//            t.join();
//            System.out.println("Completed");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        Scanner sc = new Scanner(System.in);
//        ServerSocket server = new ServerSocket(3010);
//        Socket socket = server.accept();
//        DataInputStream dis = new DataInputStream(socket.getInputStream());
//        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//        InputStream is = socket.getInputStream();
//        while (true) {
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//            String filename = dis.readUTF();
//            while ((bytesRead = is.read(buffer)) != -1) {
//                FileOutputStream fos = new FileOutputStream("D:\\folder\\1\\" + filename + "_copy.txt");
//                fos.write(buffer, 0, bytesRead);
//                dos.writeUTF(filename + "_copy.txt OK");
//                filename = dis.readUTF();
//                if (filename.equals("q")) break;
//                fos.close();
//            }
//            System.out.println("Done");
//        }

//        System.out.println("Main complete");
//        Date date = new SimpleDateFormat("yyyyMMdd_HHmmss").parse("2023-12-09 21:54:51");
//        System.out.println(date);
//        String p1 = "D:\\contain_file_receive";
//        String p2 = "D:\\contain_file_receive_copy";
//        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(p1))) {
//            for (Path entry : stream) {
//                System.out.println(entry);
//                Path newTarget = Paths.get(p2).resolve(entry.getFileName());
//                    Files.copy(entry, newTarget, StandardCopyOption.REPLACE_EXISTING);
//                }
//            }
//        Pattern p = Pattern.compile("\\d+");
//        Matcher m = p.matcher("server (12)");
//        m.find();
//        int running = Integer.parseInt(m.group());
//        System.out.println(running + 1);
    }
}


