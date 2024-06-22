package com.vku.email.model;

public class Account {
    private String email_address;
    private String fullname;
    private String password;
    private String created_at;

    public Account(String email_address, String fullname, String password) {
        this.email_address = email_address;
        this.fullname = fullname;
        this.password = password;
    }

    public Account() {
        email_address = "";
        fullname = "";
        password = "";
        created_at = "";
    }

    public String getEmail_address() {
        return email_address;
    }
    public String getFullname() {
        return fullname;
    }
    public String getPassword() {
        return password;
    }
    public void setEmail_address(String email_address) {
        this.email_address = email_address;
    }
    public void setFullname(String username) {
        this.fullname = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getCreated_at() {
        return created_at;
    }
    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }
}
