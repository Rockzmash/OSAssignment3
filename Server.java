import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final List<ClientHandler> clients = new ArrayList<ClientHandler>();
    private static Sudoku sudoku = new Sudoku();
    private static int clientIdCounter = 1;
    private static Map<Integer, Integer> clientUpdateCounts = new HashMap<>();
    private static Map<String, Integer> boardUpdates = new HashMap<>();

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);
        sudoku.fillValues();

        System.out.println("Sudoku board Created: ");
        System.out.println(sudoku.getSudokuString());

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Server is listening on port " + portNumber);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New Client Connection");
                int clientId = clientIdCounter++;
                clientUpdateCounts.put(clientId, 0);
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    } // main

    static void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    static void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    static boolean handleUpdate(int i, int j, int n) {
        String key = i + "-" + j;
        if (sudoku.enterNumber(i, j, n)) {

            if(boardUpdates.containsKey(key)) {
                int previousClientId = boardUpdates.get(key);
                clientUpdateCounts.put(previousClientId, clientUpdateCounts.get(previousClientId) - 1);
            }
            boardUpdates.put(key, clientId);
            clientUpdateCounts.put(clientId, clientUpdateCounts.get(clientId) + 1);

            broadcast("Board Updated:\n" + sudoku.getSudokuString());
            if (sudoku.isBoardFull()) {
                announceWinner();
                System.exit(0);
            }
            return true;
        }
        return false;
    }

    static void announceWinner() {
        int maxUpdates = 0;
        int winnerId = -1;

        for(Map.Entry<Integer, Integer> entry : clientUpdateCounts.entrySet()) {
            if (entry.getValue() > maxUpdates) {
                maxUpdates = entry.getValue();
                winnerId = entry.getKey();
            }
        }

        if(winnerId != -1) {
            broadcast("Game Complete! The winner is Client " + winnerId + " with " + maxUpdates + " updates.");
        } else {
            broadcast("Game Complete! No winner determined.");
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final int clientId;
        private PrintWriter out;

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("Welcome to the Sudoku server! Your client ID is " + clientId);
                out.println("Current Sudoku board:\n" + sudoku.getSudokuString());

                String inputLine;
                while ((inputLine = in.readLine())!= null) {
                    String[] parts = inputLine.split(" ");
                    if (parts[0].equalsIgnoreCase("show")) {
                        out.println("Current Sudoku board:\n" + sudoku.getSudokuString());
                    } else if (parts[0].equalsIgnoreCase("update") && parts.length == 4) {
                        try {
                            int i = Integer.parseInt(parts[1]);
                            int j = Integer.parseInt(parts[2]);
                            int n = Integer.parseInt(parts[3]);
                            if (handleUpdate(i, j, n)) {
                                out.println("Update successful!");
                            } else {
                                out.println("Update failed! Invalid move.");
                            }
                        } catch (NumberFormatException e){
                            out.println("Invalid input. Use: update <i> <j> <n>");
                        } 
                    } else {
                            out.println("Invalid command. Use 'show' or 'update <i> <j> <n>'");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error occurred with a client connection.");
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.err.println("Error closing client socket.");
                    }
                    removeClient(this);
                    System.out.println("Client disconnected.");
                }
            }

            void sendMessage (String message) {
                if (out != null) {
                    out.println(message);
                }
            }
    
    }

}
