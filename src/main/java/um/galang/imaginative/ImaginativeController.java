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
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
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
import java.util.HashMap;
import java.util.Properties;
import javafx.embed.swing.SwingFXUtils;

public class ImaginativeController {
    @FXML
    public ImageView ImageContainer;
    @FXML
    private TextField PromptField;
    static HttpResponse<String> response;
    static libraryFunction lib = new libraryFunction();
    static String extractedURL;
    public static int currentIndex = 0;
    ArrayList<Image> images = new ArrayList<>();
    ArrayList<String> imagesUrl = new ArrayList<>();

    HashMap<Image, String> metadata = new HashMap<>();

    @FXML
    private Button backGenerate;
    @FXML
    private Button nextGenerate;

    @FXML
    public void initialize() {
        getApiKey();
        System.out.println("Retrieved API Key: " + retrievedAPI);
        System.out.println();
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
    int generateSession = 0;
    String apiKey, retrievedAPI;
    void getApiKey() {
        // Reading the api_key from the api_keys.properties file (Locally Stored for Security Purposes).
        Properties prop = new Properties();
        // Reading the api_keys.properties file and returning the api_key property.
        try (InputStream input = new FileInputStream("src/main/api_keys.properties")) {
            // Reading the api_key from the properties file.
            prop.load(input);
            retrievedAPI = prop.getProperty("api_key");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    @FXML
    void saveImage(ActionEvent event) throws IOException {
        saveRetrievedImage();
    }
    private static final String FILE_EXTENSION = "png";
    void saveRetrievedImage() throws IOException {
        // Load the image from a URL
        BufferedImage image = SwingFXUtils.fromFXImage(ImageContainer.getImage(), null);

        // Create a file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));

        // Show the file chooser and get the user's response
        int userSelection = fileChooser.showSaveDialog(null);

        // If the user selected a file
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            // Get the selected file
            File fileToSave = fileChooser.getSelectedFile();

            // Ensure that the file has the correct extension
            if (!fileToSave.getName().endsWith(".png")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".png");
            }

            // Save the image to the selected file
            try {
                ImageIO.write(image, "png", fileToSave);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error saving image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

    }

    @FXML
    void GenerateImaginative(ActionEvent event) {
        generateSession++;
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
            JOptionPane jOptionPane = new JOptionPane();
            // Set up a listener to update the UI when the task is complete
            task.setOnSucceeded(action -> {
                String imageUrl = task.getValue();
                URL url = null;
                try {
                    url = new URL(imageUrl);
                } catch (MalformedURLException e) {
                    JOptionPane.showMessageDialog(null, "An error occurred while communicating with the API Server", "Imaginative Error", JOptionPane.ERROR_MESSAGE);
                    newScene.setCursor(Cursor.DEFAULT);
                    throw new RuntimeException(e);
                }
                try (InputStream inputStream = new BufferedInputStream(url.openStream())) {
                    Image image = new Image(inputStream);
                    metadata.put(image, extractedURL);
                    imagesUrl.add(extractedURL);
                    images.add(image);
                    ImageContainer.setImage(image);
                    newScene.setCursor(Cursor.DEFAULT);
                } catch (IOException e) {
                    newScene.setCursor(Cursor.DEFAULT);
                    JOptionPane.showMessageDialog(null, "An error occurred while communicating with the API Server", "Imaginative Error", JOptionPane.ERROR_MESSAGE);
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
        apiKey = retrievedAPI;
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
        String url = null;
        // Create a JsonParser instance
        JsonParser parser = new JsonParser();
        // Parse the JSON data
        JsonObject root = parser.parse(JSONResponse).getAsJsonObject();
        if (root.has("error")) {
            JOptionPane.getRootFrame().dispose();
        } else {
            url = root.getAsJsonArray("data").get(0).getAsJsonObject().get("url").getAsString();
        }
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
}