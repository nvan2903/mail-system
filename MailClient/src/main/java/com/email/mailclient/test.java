package com.email.mailclient;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;

public class test extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hover Popup Example");

        Label label = new Label("Hover Me");

        Popup popup = new Popup();
        Label popupLabel = new Label("This is a Popup!");
        popupLabel.setStyle("-fx-background-color: white; -fx-padding: 10px;");
        popup.getContent().add(popupLabel);

        label.setOnMouseEntered(event -> {
            popup.show(primaryStage);
        });

        label.setOnMouseExited(event -> {
            popup.hide();
        });

        StackPane root = new StackPane();
        root.getChildren().add(label);

        Scene scene = new Scene(root, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
