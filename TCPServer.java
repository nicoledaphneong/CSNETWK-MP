import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class TCPServer {
    private static Map<String, String> aliases = new HashMap<>();

    public static void main(String argv[]) throws Exception {
        int port = Integer.parseInt(argv[0]);
        ServerSocket welcomeSocket = new ServerSocket(port);

        System.out.println("Server is running...");

        while (true) {
            Socket connectionSocket = welcomeSocket.accept();
            new Thread(new ClientHandler(connectionSocket)).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
                DataInputStream inStream = new DataInputStream(socket.getInputStream());
                String clientSentence;
                String alias = null;
                while (true) {
                    clientSentence = inFromClient.readLine();
                    if (clientSentence == null) {
                        System.out.println((alias != null ? alias : socket.getRemoteSocketAddress()) + ": Client disconnected");
                        break;
                    } else if (clientSentence.startsWith("/register ")) {
                        String requestedAlias = clientSentence.split(" ", 2)[1];
                        if (aliases.containsValue(requestedAlias)) {
                            outToClient.writeBytes("Error: Registration failed. Handle or alias already exists.\n");
                        } else {
                            alias = requestedAlias;
                            aliases.put(socket.getRemoteSocketAddress().toString(), alias);
                            outToClient.writeBytes("Welcome " + alias + "!\n");
                            System.out.println(alias + ": Client registered");
                        }
                    } else if (clientSentence.startsWith("/join")) {
                        System.out.println(socket.getRemoteSocketAddress() + ": Joined the server");
                    } else if (clientSentence.startsWith("/store ")) {
                        String filename = clientSentence.split(" ", 2)[1];
                        int size = inStream.readInt();
                        byte[] data = new byte[size];
                        inStream.readFully(data, 0, size);
                        Files.write(Paths.get(filename), data);
                        System.out.println((alias != null ? alias : socket.getRemoteSocketAddress()) + ": Uploaded " + filename);
                    } else if (clientSentence.equals("/dir")) {
                        File dir = new File(".");
                        File[] files = dir.listFiles();
                        outToClient.writeInt(files.length);
                        for (File file : files) {
                            outToClient.writeBytes(file.getName() + '\n');
                        }
                    } else {
                        System.out.println("FROM CLIENT: " + clientSentence);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}