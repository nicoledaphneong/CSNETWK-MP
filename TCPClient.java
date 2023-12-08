import java.io.*;
import java.net.*;
import java.util.*;

class TCPClient {
    public static void main(String argv[]) throws Exception {
        String serverIp = null;
        int port = 0;
        Socket clientSocket = null;
        DataOutputStream outToServer = null;
        String alias = null;

        Scanner scanner = new Scanner(System.in);

        System.out.println("Client is running...");

        while (true) {
            String sentence = scanner.nextLine();
            if (sentence.equals("/?")) {
                System.out.println("/join <server_ip_add> <port> - Connect to the server application");
                System.out.println("/leave - Disconnect to the server application");
                System.out.println("/register <handle> - Register a unique handle or alias");
                System.out.println("/store <filename> - Send file to server");
                System.out.println("/dir - Request directory file list from a server");
                System.out.println("/get <filename> - Fetch a file from a server");
                System.out.println("/? - Request command help to output all Input Syntax commands for references");
            } else if (sentence.startsWith("/join ")) {
                String[] parts = sentence.split(" ");
                if (parts.length == 3) {
                    serverIp = parts[1];
                    port = Integer.parseInt(parts[2]);
                    try {
                        clientSocket = new Socket(serverIp, port);
                        outToServer = new DataOutputStream(clientSocket.getOutputStream());
                        new Thread(new ServerHandler(clientSocket)).start();
                        System.out.println("Connection to the File Exchange Server is successful!");
                    } catch (IOException e) {
                        System.out.println("Error: Connection to the Server has failed! Please check IP Address and Port Number.");
                    }
                } else {
                    System.out.println("Error: Connection to the Server has failed! Please check IP Address and Port Number.");
                }
            } else if (sentence.startsWith("/register ")) {
                if (clientSocket == null) {
                    System.out.println("Error: Registration failed. Handle or alias already exists.");
                    continue;
                }
                alias = sentence.split(" ", 2)[1];
                outToServer.writeBytes(sentence + '\n');

            } else if (sentence.equals("/leave")) {
                if (clientSocket == null) {
                    System.out.println("Error: Disconnection failed. Please connect to the server first.");
                    continue;
                }
                clientSocket.close();
                System.out.println("Connection closed. Thank you!");
                break;
            } else if (clientSocket != null) {
                outToServer.writeBytes((alias != null ? alias + ": " : "") + sentence + '\n');
            }
        }
    }

    static class ServerHandler implements Runnable {
        private Socket socket;

        public ServerHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while (true) {
                    String serverSentence = inFromServer.readLine();
                    System.out.println("FROM SERVER: " + serverSentence);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}