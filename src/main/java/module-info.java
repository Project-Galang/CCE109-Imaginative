module com.example.imaginative {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires java.desktop;
    requires com.google.gson;


    opens um.galang.imaginative to javafx.fxml;
    exports um.galang.imaginative;
}