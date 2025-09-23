import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

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
    private Thread readerThread;

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

        HBox topBox = new HBox(8, new Label("Server:"), serverField, new Label("Port:"), portField, new Label("Name:"), nameField, connectBtn);
        topBox.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(messageArea);
        root.setBottom(inputBox);

        Scene scene = new Scene(root, 700, 500);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Event handlers
        connectBtn.setOnAction(e -> connect());
        sendBtn.setOnAction(e -> sendMessage());

        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) sendMessage();
        });

        primaryStage.setOnCloseRequest(e -> {
            disconnect();
            Platform.exit();
        });
    }

    private void connect() {
        String server = serverField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());
        String name = nameField.getText().trim();
        if (server.isEmpty() || name.isEmpty()) {
            showAlert("Server and name must be provided.");
            return;
        }

        try {
            socket = new Socket(server, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Start reader thread
            readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        final String l = line;
                        if (l.startsWith("SUBMITNAME")) {
                            out.println(name);
                        } else if (l.startsWith("NAMEINUSE")) {
                            Platform.runLater(() -> showAlert("Name already in use. Choose another and reconnect."));
                            disconnect();
                            break;
                        } else if (l.startsWith("NAMEACCEPTED")) {
                            Platform.runLater(() -> {
                                sendBtn.setDisable(false);
                                messageArea.appendText("[Connected as " + name + "]\n");
                            });
                        } else {
                            Platform.runLater(() -> messageArea.appendText(l + "\n"));
                        }
                    }
                } catch (IOException ex) {
                    Platform.runLater(() -> messageArea.appendText("Connection closed." + "\n"));
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            connectBtn.setDisable(true);
            nameField.setDisable(true);
            serverField.setDisable(true);
            portField.setDisable(true);

        } catch (IOException ex) {
            showAlert("Cannot connect: " + ex.getMessage());
        }
    }

    private void sendMessage() {
        if (out == null) return;
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        out.println(text);
        inputField.clear();
    }

    private void disconnect() {
        try {
            if (out != null) out.println("/quit");
            if (socket != null) socket.close();
        } catch (IOException e) {
            // ignore
        } finally {
            out = null;
            in = null;
            socket = null;
            sendBtn.setDisable(true);
            connectBtn.setDisable(false);
            nameField.setDisable(false);
            serverField.setDisable(false);
            portField.setDisable(false);
        }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
