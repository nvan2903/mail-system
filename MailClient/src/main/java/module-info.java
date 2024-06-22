module com.email.mailclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.web;
    requires com.google.gson;

    opens com.email.mailclient.model to com.google.gson, javafx.base;
    opens com.email.mailclient to javafx.fxml;
    opens com.email.mailclient.controllers to javafx.fxml;  // Add this line to open the controllers package
    exports com.email.mailclient;
}
