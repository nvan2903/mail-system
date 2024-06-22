package com.email.mailclient.model;


public class EmailAccount {

    private String email_address;
    private String fullname;
    private String password;
    private String created_at;


    public String getEmail_address() {
        return email_address;
    }

    public void setEmail_address(String email_address) {
        this.email_address = email_address;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getPassword() {
        return password;
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
