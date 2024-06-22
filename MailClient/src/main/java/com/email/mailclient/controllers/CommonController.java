package com.email.mailclient.controllers;


import javafx.stage.Stage;

public abstract class CommonController {

    private String fxml;
    private Stage stage;





    public String getFxml() {
        return fxml;
    }

    public Stage getStage() {
        return stage;
    }
}
