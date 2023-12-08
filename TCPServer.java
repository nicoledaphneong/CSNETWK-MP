import java.io.*;
import java.net.*;
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
                while (true) {
                    String clientSentence = inFromClient.readLine();
                    if (clientSentence == null) {
                        System.out.println((alias != null ? alias : "Client " + socket.getRemoteSocketAddress()) + " has disconnected.");
                        clients.remove(socket);
                        socket.close();
                        break;
                    }
                    if (clientSentence.startsWith("/register ")) {
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