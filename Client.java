import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
         
        if (args.length != 2) {
            System.err.println(
                "Usage: java Client <host name> <port number>");
            System.exit(1);
        }
 
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
 
        try (
            Socket socket = new Socket(hostName, portNumber);
            PrintWriter out =
                new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in =
                new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            BufferedReader stdIn =
                new BufferedReader(
                    new InputStreamReader(System.in))
        ) {
            while (true) {
                System.out.println("Enter a command (show or update i j num), or type 'exit' to quit:");
                String userInput = scanner.nextLine();

                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }

                if (validateInput(userInput)) {
                    out.println(userInput);
                    String serverResponse = in.readLine();
                    System.out.println("Server: " + serverResponse);
                } else {
                    System.out.println("Invalid input. Try again.");
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                hostName);
            System.exit(1);
        } 
    }

    private static boolean validateInput(String input) {
        String[] parts = input.split(" ");
        if (parts[0].equalsIgnoreCase("show") && parts.length == 1) {
            return true;
        }
        if (parts[0].equalsIgnoreCase("update") && parts.length == 4) {
            try {
                Integer.parseInt(parts[1]);
                Integer.parseInt(parts[2]);
                Integer.parseInt(parts[3]);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}