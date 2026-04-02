module org.example.baitaplon {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;


    opens org.example.baitaplon to javafx.fxml, com.fasterxml.jackson.databind;
    exports org.example.baitaplon;
    exports models;
    opens models to com.fasterxml.jackson.databind, javafx.fxml;
    exports controllers;
    opens controllers to com.fasterxml.jackson.databind, javafx.fxml;
}
