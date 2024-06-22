package com.vku.email.test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class serverTest {
    static final Map<String, Semaphore> semaphoreMap = new HashMap<>();
    public static void main(String[] args) throws IOException{
//        ServerSocket server1 = new ServerSocket(25);
//        ServerSocket server2 = new ServerSocket(21);
//
//        Thread t1 = new Thread(() -> {
//            while (true) {
//                try {
//                    Socket socket = server1.accept();
//                    System.out.println("port 25 accept " + socket);
//                    Execute25 e25 = new Execute25(socket, semaphoreMap);
//                    e25.start();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });
//        Thread t2 = new Thread(() -> {
//            while (true) {
//                try {
//                    Socket socket = server2.accept();
//                    System.out.println("port 21 accept " + socket);
//                    Execute21 e21 = new Execute21(socket, semaphoreMap);
//                    e21.start();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });
//        t1.start();
//        t2.start();
        ServerSocket server = new ServerSocket(3010);
        Socket socket = server.accept();
        InputStream is = socket.getInputStream();
        DataInputStream dis = new DataInputStream(socket.getInputStream());
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        byte[] buffer = new byte[1024];
        int bytesRead;
        while (true) {
            String filename = dis.readUTF();
            while ((bytesRead = is.read(buffer)) != -1) {
                FileOutputStream fos = new FileOutputStream("D:\\folder\\" + filename);
                fos.write(buffer, 0, bytesRead);
                dos.writeUTF("OK");
                fos.close();
                break;
            }
        }
    }
}

class Execute25 extends Thread {

    private final Socket socket;
    private final Map<String, Semaphore> semaphoreMap;
    public Execute25(Socket socket, Map<String, Semaphore> semaphoreMap) {
        this.socket = socket;
        this.semaphoreMap = semaphoreMap;
    }

    @Override
    public void run() {
        String email;
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            email = dis.readUTF();
            semaphoreMap.put(email, new Semaphore(0));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i=0; i<5; i++) {
            System.out.println(i + " " + socket);
            if (i == 3) {
                try {
                    semaphoreMap.get(email).acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
class Execute21 extends Thread {

    private final Socket socket;
    private final Map<String, Semaphore> semaphoreMap;
    public Execute21(Socket socket, Map<String, Semaphore> semaphoreMap) {
        this.socket = socket;
        this.semaphoreMap = semaphoreMap;
    }

    @Override
    public void run() {
        DataInputStream dos = null;
        try {
            dos = new DataInputStream(socket.getInputStream());
            String email = dos.readUTF();
            System.out.println("User start transport file");
            for (int i=0; i<5; i++) {
                System.out.println(i + " " + socket);
            }
            semaphoreMap.get(email).release();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}