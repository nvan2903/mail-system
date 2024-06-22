module com.vku.email {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires com.google.gson;


    opens com.vku.email to javafx.fxml;
    opens com.vku.email.model to com.google.gson;
    exports com.vku.email;
    exports com.vku.email.test;
    opens com.vku.email.test to javafx.fxml;
}