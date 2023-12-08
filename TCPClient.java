import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class TCPClient {
    public static void main(String argv[]) throws Exception {
        boolean isConnected = false;
        String serverIp = null;
        int port = 0;
        Socket clientSocket = null;
        DataOutputStream outToServer = null;
        String alias = null;

        Scanner scanner = new Scanner(System.in);

        System.out.println("Client is running...");

        while (true) {
            String sentence = scanner.nextLine();
            if (sentence.trim().isEmpty()) {
                continue;
            }
        
            if (clientSocket == null && !sentence.startsWith("/join")) {
                System.out.println("Error: Command failed. Please connect to the server first.");
                continue;
            }
        
            if (sentence.startsWith("/join")) {
                String[] parts = sentence.split(" ");
                if (parts.length != 3) {
                    System.out.println("Error: Command parameters do not match or is not allowed.");
                    continue;
                }
                if (isConnected) {
                    System.out.println("Error: You are already connected to the server.");
                    continue;
                }
                serverIp = parts[1];
                port = Integer.parseInt(parts[2]);
                try {
                    clientSocket = new Socket(serverIp, port);
                    outToServer = new DataOutputStream(clientSocket.getOutputStream());
                    new Thread(new ServerHandler(clientSocket)).start();
                    isConnected = true; // Set isConnected to true after successfully connecting to the server
                    System.out.println("Connection to the File Exchange Server is successful!");
                } catch (IOException e) {
                    System.out.println("Error: Connection to the Server has failed! Please check IP Address and Port Number.");
                }
            }
        
            if (sentence.equals("/?") || sentence.equals("/leave") || sentence.startsWith("/register ") || sentence.startsWith("/store ") || sentence.equals("/dir") || sentence.startsWith("/get ")) {
                if (sentence.equals("/?")) {
                    System.out.println("/join <server_ip_add> <port> - Connect to the server application");
                    System.out.println("/leave - Disconnect to the server application");
                    System.out.println("/register <handle> - Register a unique handle or alias");
                    System.out.println("/store <filename> - Send file to server");
                    System.out.println("/dir - Request directory file list from a server");
                    System.out.println("/get <filename> - Fetch a file from a server");
                    System.out.println("/? - Request command help to output all Input Syntax commands for references");
                } else if (sentence.startsWith("/register ")) {
                    if (clientSocket == null) {
                        System.out.println("Error: Registration failed. Handle or alias already exists.");
                        continue;
                    }
                    alias = sentence.split(" ", 2)[1];
                    outToServer.writeBytes(sentence + '\n');
                } else if (sentence.startsWith("/store ")) {
                    if (clientSocket == null) {
                        System.out.println("Error: Please connect to the server first.");
                        continue;
                    }
                    if (alias == null) {
                        System.out.println("Error: Please register first.");
                        continue;
                    }
                    String filename = sentence.split(" ", 2)[1];
                    try {
                        Path path = Paths.get(filename);
                        byte[] data = Files.readAllBytes(path);
                
                        outToServer.writeBytes(sentence + ' ' + data.length + '\n');
                        outToServer.write(data, 0, data.length);
                
                        System.out.println("File " + filename + " has been sent to the server.");
                    } catch (IOException e) {
                        System.out.println("Error: File not found.");
                    }
                } else if (sentence.equals("/dir")) {
                    if (clientSocket == null) {
                        System.out.println("Error: Please connect to the server first.");
                        continue;
                    }
                    if (alias == null) {
                        System.out.println("Error: Please register first.");
                        continue;
                    }
                    System.out.println("Server Directory");
                    outToServer.writeBytes(sentence + '\n');
                } else if (sentence.startsWith("/get ")) {
                    if (clientSocket == null) {
                        System.out.println("Error: Please connect to the server first.");
                        continue;
                    }
                    if (alias == null) {
                        System.out.println("Error: Please register first.");
                        continue;
                    }
                    String filename = sentence.split(" ", 2)[1];
                    outToServer.writeBytes(sentence + '\n');
                    try {
                        Path path = Paths.get("ServerFiles/" + filename);
                        byte[] data = Files.readAllBytes(path);
                        Files.write(Paths.get(filename), data);
                        System.out.println("File received from Server: " + filename);
                    } catch (IOException e) {
                        System.out.println("Error: File not found in the server.");
                    }
                } else if (sentence.equals("/leave")) {
                    if (clientSocket == null) {
                        System.out.println("Error: Disconnection failed. Please connect to the server first.");
                        continue;
                    }
                    clientSocket.close();
                    System.out.println("Connection closed. Thank you!");
                    break;
                } else {
                    System.out.println("Error: Command not found.");
                }
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
                DataInputStream inStream = new DataInputStream(socket.getInputStream());
                while (true) {
                    String serverSentence = inFromServer.readLine();
                    if (serverSentence == null) {
                        System.out.println("Server has disconnected.");
                        socket.close();
                        break;
                    } else if (serverSentence.startsWith("/dir ")) {
                        int count = inStream.readInt();
                        System.out.println("Directory listing:");
                        for (int i = 0; i < count; i++) {
                            System.out.println(inFromServer.readLine());
                        }
                    } else if (serverSentence.startsWith("Stored file ")) {
                        System.out.println(serverSentence);
                    } else {
                        System.out.println(serverSentence);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}