module org.example.baitaplon {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;


    opens org.example.baitaplon to javafx.fxml, com.fasterxml.jackson.databind;
    exports org.example.baitaplon;
}
