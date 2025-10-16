import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatClientFX extends Application {
    private TextArea messageArea;
    private TextField inputField;
    private Button sendBtn;
    private TextField serverField;
    private TextField portField;
    private TextField nameField;
    private Button connectBtn;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JavaFX Chat Client");

       
        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);

     
        inputField = new TextField();
        inputField.setPromptText("Type a message...");
        sendBtn = new Button("Send");
        sendBtn.setDisable(true);

        HBox inputBox = new HBox(8, inputField, sendBtn);
        inputBox.setPadding(new Insets(8));

       
        serverField = new TextField("127.0.0.1");
        serverField.setPrefWidth(120);
        portField = new TextField("12345");
        portField.setPrefWidth(80);
        nameField = new TextField();
        nameField.setPromptText("Your name");
        connectBtn = new Button("Connect");

        HBox topBox = new HBox(8,
                new Label("Server:"), serverField,
                new Label("Port:"), portField,
                new Label("Name:"), nameField,
                connectBtn);
        topBox.setPadding(new Insets(8));

        // Layout
        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(messageArea);
        root.setBottom(inputBox);

        Scene scene = new Scene(root, 700, 500);
        primaryStage.setScene(scene);
        primaryStage.show();


        connectBtn.setOnAction(e -> connect());
        sendBtn.setOnAction(e -> sendMessage());
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) sendMessage();
        });

        primaryStage.setOnCloseRequest(e -> disconnect());
    }

    private void connect() {
        String server = serverField.getText().trim();
        String name = nameField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("Port must be a number.");
            return;
        }

        if (server.isEmpty() || name.isEmpty()) {
            showAlert("Server and Name must be provided.");
            return;
        }

        try {
            socket = new Socket(server, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Start thread to read messages from server
            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        final String msg = line;
                        Platform.runLater(() -> messageArea.appendText(msg + "\n"));
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> messageArea.appendText("[Server disconnected]\n"));
                }
            });
            reader.setDaemon(true);
            reader.start();

            // Send username to server
            out.println(name);

            sendBtn.setDisable(false);
            connectBtn.setDisable(true);
            serverField.setDisable(true);
            portField.setDisable(true);
            nameField.setDisable(true);

            messageArea.appendText("[Connected to server]\n");

        } catch (IOException e) {
            showAlert("Cannot connect to server: " + e.getMessage());
        }
    }

    private void sendMessage() {
        if (out == null) return;
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;

        out.println(msg);
        inputField.clear();
    }

    private void disconnect() {
        try {
            if (out != null) out.println("/quit");
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        finally {
            sendBtn.setDisable(true);
            connectBtn.setDisable(false);
            serverField.setDisable(false);
            portField.setDisable(false);
            nameField.setDisable(false);
            messageArea.appendText("[Disconnected]\n");
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
