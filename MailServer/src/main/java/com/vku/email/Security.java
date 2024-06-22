package com.vku.email;

import java.security.MessageDigest;

public class Security {
    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] inputBytes = input.getBytes();
            byte[] hash = digest.digest(inputBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
