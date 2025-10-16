import java.io.*;
import java.net.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatServer {
    private static final int PORT = 12345;
    private static final Map<String, PrintWriter> clients = Collections.synchronizedMap(new LinkedHashMap<>());

    public static void main(String[] args) throws Exception {
        System.out.println("Chat server running on port " + PORT);
        ServerSocket serverSocket = new ServerSocket(PORT);

        try {
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } finally {
            serverSocket.close();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private String username;
        private BufferedReader in;
        private PrintWriter out;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("SUBMITNAME");
                username = in.readLine();
                if (username == null) return;

                synchronized (clients) {
                    while (clients.containsKey(username)) {
                        out.println("NAMEINUSE");
                        username = in.readLine();
                        if (username == null) return;
                    }
                    clients.put(username, out);
                }

                out.println("NAMEACCEPTED " + username);
                broadcast("[Server] " + username + " joined");
                sendUserList();

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/quit")) break;
                    if (message.startsWith("/pm ")) {
                        handlePrivateMessage(message);
                    } else if (message.startsWith("/file ")) {
                        handleFileMessage(message);
                    } else {
                        String encryptedMsg = EncryptionUtil.encrypt(message);
                        Message msg = new Message(encryptedMsg, username, Message.MessageType.PUBLIC, true);
                        broadcast(msg.toString());
                    }
                }
            } catch (IOException e) {
                System.out.println("Client error: " + e.getMessage());
            } finally {
                if (username != null) {
                    clients.remove(username);
                    broadcast("[Server] " + username + " left");
                    sendUserList();
                }
                try { socket.close(); } catch (IOException e) {}
            }
        }

        private void broadcast(String msg) {
            System.out.println(msg);
            synchronized (clients) {
                for (PrintWriter pw : clients.values()) {
                    pw.println(msg);
                }
            }
        }

        private void sendUserList() {
            StringBuilder userList = new StringBuilder("USERLIST ");
            synchronized (clients) {
                for (String user : clients.keySet()) {
                    userList.append(user).append(",");
                }
            }
            if (userList.length() > 9) { // Remove trailing comma
                userList.setLength(userList.length() - 1);
            }
            broadcast(userList.toString());
        }

        private void handlePrivateMessage(String message) {
            String[] parts = message.split(" ", 3);
            if (parts.length < 3) return;
            String targetUser = parts[1];
            String pmMessage = parts[2];

            synchronized (clients) {
                PrintWriter targetOut = clients.get(targetUser);
                if (targetOut != null) {
                    String encryptedPm = EncryptionUtil.encrypt(pmMessage);
                    Message pmMsg = new Message(encryptedPm, username, Message.MessageType.PRIVATE, true);
                    targetOut.println(pmMsg.toString());
                    // Also send to sender for confirmation
                    out.println(pmMsg.toString());
                } else {
                    out.println("[Server] User " + targetUser + " not found.");
                }
            }
        }

        private void handleFileMessage(String message) {
            // Simplified file sharing: just broadcast file name for now
            String[] parts = message.split(" ", 2);
            if (parts.length < 2) return;
            String fileName = parts[1];
            Message fileMsg = new Message(fileName, username, Message.MessageType.FILE);
            broadcast(fileMsg.toString());
        }
    }
}
