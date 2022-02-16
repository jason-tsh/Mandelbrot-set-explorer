module com.example.mp2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens implementation to javafx.fxml;
    exports implementation.delegate;
    opens implementation.delegate to javafx.fxml;
    exports implementation.model;
    opens implementation.model to javafx.fxml;
    exports implementation;
}