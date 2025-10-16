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
    private ListView<String> userList;
    private Button emojiBtn;
    private Button fileBtn;
    private Button themeBtn;
    private TabPane tabPane;
    private Map<String, TextArea> privateChats;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isDarkTheme = false;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JavaFX Chat Client");

        // Initialize components
        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);

        inputField = new TextField();
        inputField.setPromptText("Type a message...");
        sendBtn = new Button("Send");
        sendBtn.setDisable(true);

        emojiBtn = new Button("😊");
        fileBtn = new Button("📎");
        themeBtn = new Button("🌙");

        HBox inputBox = new HBox(8, inputField, sendBtn, emojiBtn, fileBtn, themeBtn);
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

        // User list
        userList = new ListView<>();
        userList.setPrefWidth(150);
        userList.setOnMouseClicked(e -> {
            String selectedUser = userList.getSelectionModel().getSelectedItem();
            if (selectedUser != null && !selectedUser.equals(nameField.getText())) {
                openPrivateChat(selectedUser);
            }
        });

        // Tab pane for chats
        tabPane = new TabPane();
        Tab publicTab = new Tab("Public", messageArea);
        publicTab.setClosable(false);
        tabPane.getTabs().add(publicTab);

        privateChats = new HashMap<>();

        // Layout
        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(tabPane);
        root.setRight(userList);
        root.setBottom(inputBox);

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Event handlers
        connectBtn.setOnAction(e -> connect());
        sendBtn.setOnAction(e -> sendMessage());
        emojiBtn.setOnAction(e -> insertEmoji());
        fileBtn.setOnAction(e -> sendFile());
        themeBtn.setOnAction(e -> toggleTheme(scene));
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
                        Platform.runLater(() -> handleIncomingMessage(msg));
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

    private void handleIncomingMessage(String msg) {
        if (msg.startsWith("USERLIST ")) {
            String[] parts = msg.split(" ", 2);
            if (parts.length > 1) {
                String[] users = parts[1].split(",");
                userList.getItems().clear();
                for (String user : users) {
                    userList.getItems().add(user);
                }
            }
        } else {
            // Decrypt if encrypted
            if (msg.contains(":")) {
                String[] parts = msg.split(": ", 2);
                if (parts.length > 1) {
                    String decrypted = EncryptionUtil.decrypt(parts[1]);
                    msg = parts[0] + ": " + decrypted;
                }
            }
            messageArea.appendText(msg + "\n");
        }
    }

    private void openPrivateChat(String user) {
        if (privateChats.containsKey(user)) {
            // Switch to existing tab
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getText().equals(user)) {
                    tabPane.getSelectionModel().select(tab);
                    break;
                }
            }
        } else {
            TextArea privateArea = new TextArea();
            privateArea.setEditable(false);
            privateArea.setWrapText(true);
            Tab privateTab = new Tab(user, privateArea);
            privateTab.setOnClosed(e -> privateChats.remove(user));
            tabPane.getTabs().add(privateTab);
            privateChats.put(user, privateArea);
            tabPane.getSelectionModel().select(privateTab);
        }
    }

    private void insertEmoji() {
        inputField.appendText("😊");
    }
