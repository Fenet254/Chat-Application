import java.io.*;
import java.net.*;
import java.util.*;

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

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/quit")) break;
                    broadcast(username + ": " + message);
                }
            } catch (IOException e) {
                System.out.println("Client error: " + e.getMessage());
            } finally {
                if (username != null) {
                    clients.remove(username);
                    broadcast("[Server] " + username + " left");
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
    }
}
