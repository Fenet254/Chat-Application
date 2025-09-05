import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class ChatClientGUI extends Application {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1234;

    private PrintWriter out;
    private BufferedReader in;

    private TextArea chatArea;
    private TextField inputField;
    private String username = "User";

    @Override
    public void start(Stage primaryStage) {
        // UI elements
        chatArea = new TextArea();
        chatArea.setEditable(false);

        inputField = new TextField();
        inputField.setPromptText("Type your message...");
        inputField.setOnAction(e -> sendMessage());

        Button sendBtn = new Button("Send");
        sendBtn.setOnAction(e -> sendMessage());

        HBox inputBox = new HBox(10, inputField, sendBtn);
        VBox root = new VBox(10, chatArea, inputBox);

        Scene scene = new Scene(root, 400, 300);

        primaryStage.setTitle("Java Chat App");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Connect to server
        connectToServer();
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Thread to listen for messages
            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        String msg = response;
                        Platform.runLater(() -> chatArea.appendText(msg + "\n"));
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> chatArea.appendText("Disconnected from server.\n"));
                }
            }).start();

        } catch (IOException e) {
            chatArea.appendText("Could not connect to server.\n");
        }
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty() && out != null) {
            out.println(username + ": " + message);
            inputField.clear();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
