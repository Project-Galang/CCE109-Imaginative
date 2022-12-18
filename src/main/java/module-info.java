module um.galang.imaginative {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires java.desktop;
    requires com.google.gson;
    requires javafx.swing;


    opens um.galang.imaginative to javafx.fxml;
    exports um.galang.imaginative;
}