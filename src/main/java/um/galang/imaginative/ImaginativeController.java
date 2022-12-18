package um.galang.imaginative;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class ImaginativeController {
    @FXML
    private ImageView ImageContainer;
    @FXML
    private TextField PromptField;
    static HttpResponse<String> response;
    static libraryFunction lib = new libraryFunction();
    static String extractedURL;
    int currentIndex = 0;
    ArrayList<Image> images = new ArrayList<>();
    @FXML
    private Button backGenerate;
    @FXML
    private Button nextGenerate;

    @FXML
    public void initialize() {
    }
    @FXML
    void backGenerate(ActionEvent event) {
        // Decrement the current index and wrap around if necessary
        currentIndex--;
        if (currentIndex < 0) {
            currentIndex = images.size() - 1;
        }
        // Set the image view to the previous image
        ImageContainer.setImage(images.get(currentIndex));
    }
    @FXML
    void nextGenerate(ActionEvent event) {
        // Increment the current index and wrap around if necessary
        currentIndex++;
        if (currentIndex >= images.size()) {
            currentIndex = 0;
        }
        // Set the image view to the next image
        ImageContainer.setImage(images.get(currentIndex));
    }
    int generateSession = 1;
    @FXML
    void GenerateImaginative(ActionEvent event) {
        new Thread( ()-> {
            // Create a new progress bar
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressBar.setMinimum(0);
            progressBar.setMaximum(100);
            JOptionPane pane = new JOptionPane(progressBar, JOptionPane.PLAIN_MESSAGE);
            pane.setOptions(new Object[] {});
            JDialog dialog = pane.createDialog(null, "Generating Imaginative Image...");
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.setVisible(true);
            dialog.setUndecorated(true);
            dialog.setAlwaysOnTop(true);
            for (int i = 0; i <= 100; i++) {
                // Simulate some work by sleeping for a while
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Update the progress bar
                progressBar.setValue(i);
            }
        }).start();


        // Perform the API request concurrently in a separate thread
        new Thread(() -> {
            // Get the current scene
            Scene newScene = ImageContainer.getScene();
            // Set the cursor to the "WAIT" cursor
            newScene.setCursor(Cursor.WAIT);

            System.out.println("Generating for Session " + generateSession);
            // Create a task to perform the API call
            Task<String> task = new Task<String>() {
                @Override
                protected String call() throws Exception {
                    return triggerRequest(PromptField.getText());
                }
            };
            // Set up a listener to update the UI when the task is complete
            task.setOnSucceeded(action -> {
                String imageUrl = task.getValue();
                URL url = null;
                try {
                    url = new URL(imageUrl);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                try (InputStream inputStream = new BufferedInputStream(url.openStream())) {
                    Image image = new Image(inputStream);
                    images.add(image);
                    ImageContainer.setImage(image);
                    newScene.setCursor(Cursor.DEFAULT);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                // Close the modal dialog
                JOptionPane.getRootFrame().dispose();
            });

            // Start the task in a background thread
            new Thread(task).start();
        }).start();
    }
    void initialRequest() throws MalformedURLException {
        String imageUrl = triggerRequest(PromptField.getText());
        URL url = new URL(imageUrl);
        try (InputStream inputStream = new BufferedInputStream(url.openStream())) {
            Image image = new Image(inputStream);
            images.add(image);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    String triggerRequest(String prompt) {
        System.out.println("API Request Communication Triggered");
        // Set the API endpoint and your API key
        String endpoint = "https://api.openai.com/v1/images/generations";
        String apiKey = "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";
        // Set the number of images to generate and the size of the image
        int n = 1;
        String size = "1024x1024";
        // Create the request body
        String body = "{\"model\":\"image-alpha-001\",\"prompt\":\"" + prompt + "\",\"num_images\":" + n + ",\"size\":\"" + size + "\"}";
        try {
            // Create the request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("Content-Type", "application/json").header("Authorization", "Bearer " + apiKey).POST(HttpRequest.BodyPublishers.ofString(body)).build();
            // Send the request and get the response
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // Print the response
            System.out.println(response.body());
            extractedURL = lib.parseUrlJSON(response.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return extractedURL;
    }

    @FXML
    private Button closeButton;
    @FXML
    void closeStage(ActionEvent event) {
        // get the stage the button belongs to
        Stage stage = (Stage) closeButton.getScene().getWindow();
        // close the stage
        stage.close();
    }
    @FXML
    private Button minimizeButton;
    @FXML
    void minimizeStage(ActionEvent event) {
        // get the stage the button belongs to
        Stage stage = (Stage) minimizeButton.getScene().getWindow();
        // minimize the stage
        stage.setIconified(true);
    }
}

class libraryFunction {
    String parseUrlJSON(String JSONResponse) {
        // Create a JsonParser instance
        JsonParser parser = new JsonParser();
        // Parse the JSON data
        JsonObject root = parser.parse(JSONResponse).getAsJsonObject();
        String url = root.getAsJsonArray("data").get(0).getAsJsonObject().get("url").getAsString();
        // Print the url value
        return url;
    }
    public static ByteArrayInputStream convertBufferedImage(BufferedImage fimage) throws IOException {
        // Create a ByteArrayOutputStream and write the image to it
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(fimage, "png", out);
        out.flush();

        // Create a ByteArrayInputStream from the output stream
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        // Create a JavaFX image from the input stream
        return in;
    }
    void saveRetrievedImage(String extractedURL) throws IOException {
        // Load the image from a URL
        BufferedImage image = ImageIO.read(new URL(extractedURL));
        convertBufferedImage(image);

        // Create a file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save File");
        fileChooser.setFileFilter(new FileNameExtensionFilter(".png", ".png"));

        // Prompt the user to select a file
        int userSelection = fileChooser.showSaveDialog(null);

        // If the user selects a file, save the image to it
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                ImageIO.write(image, ".png", fileToSave);
                // Extract the file's name and parent directory
                String fileName = fileToSave.getName();
                String parentDir = fileToSave.getParent();
                // Append the string to the filename
                String modifiedFileName = fileName + ".png";
                // Create a new File object using the modified filename and the original parent directory
            } catch (IOException e) {
                System.out.println("Error has occured while saving the image.");
            }
        }
    }
}