package um.galang.imaginative;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

public class Imaginative extends Application {
    private double x, y;
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Loading the FXML file that is being used to display the GUI.
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Imaginative-View.fxml")));
        // Setting the scene to the root and making the window transparent.
        primaryStage.setScene(new Scene(root));
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        // Allowing the user to drag the window around the screen.
        root.setOnMousePressed(event -> {
            x = event.getSceneX();
            y = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - x);
            primaryStage.setY(event.getScreenY() - y);
        });
        // Showing the stage and playing the fade in animation.
        primaryStage.setTitle("Hello!");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}