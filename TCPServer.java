import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

class TCPServer {
    private static List<Socket> clients = Collections.synchronizedList(new ArrayList<>());
    private static Set<String> aliases = Collections.synchronizedSet(new HashSet<>());

    public static void main(String argv[]) throws Exception {
        int port = Integer.parseInt(argv[0]);
        ServerSocket welcomeSocket = new ServerSocket(port);

        System.out.println("Server is running on port " + welcomeSocket.getLocalPort() + "...");

        while (true) {
            Socket connectionSocket = welcomeSocket.accept();
            clients.add(connectionSocket);
            new Thread(new ClientHandler(connectionSocket)).start();
            System.out.println("Accepted connection from " + connectionSocket.getRemoteSocketAddress() + " on server port " + connectionSocket.getLocalPort());
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private String alias;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
                InputStream inStream = socket.getInputStream();
                while (true) {
                    String clientSentence = inFromClient.readLine();
                    if (clientSentence == null) {
                        System.out.println((alias != null ? alias : "Client " + socket.getRemoteSocketAddress()) + " has disconnected.");
                        clients.remove(socket);
                        socket.close();
                        break;
                    } else if (clientSentence.startsWith("/store ")) {
                        String filename = clientSentence.split(" ", 2)[1];

                        // Receive file size from client
                        int size = inStream.read();

                        // Receive file data from client
                        byte[] data = new byte[size];
                        inStream.read(data, 0, size);

                        // Write file data to file
                        Path path = Paths.get("ServerFiles", filename);
                        Files.createDirectories(path.getParent());
                        Files.write(path, data);

                        System.out.println("Stored file " + filename);
                    } else if (clientSentence.equals("/dir")) {
                        // Get file list
                        File dir = new File(".");
                        String[] files = dir.list();

                        // Send file list to client
                        outToClient.writeBytes("/dir \n");
                        outToClient.writeInt(files.length);
                        for (String file : files) {
                            outToClient.writeBytes(file + "\n");
                        }
                    } else if (clientSentence.startsWith("/register ")) {
                        String requestedAlias = clientSentence.split(" ", 2)[1];
                        if (aliases.contains(requestedAlias)) {
                            outToClient.writeBytes("Error: Registration failed. Handle or alias already exists.\n");
                        } else {
                            alias = requestedAlias;
                            aliases.add(alias);
                            outToClient.writeBytes("Welcome " + alias + "!\n");
                            System.out.println("Welcome " + alias +"!");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}