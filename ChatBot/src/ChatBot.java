import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;

/**
 * Class to create a chat bot who responds to messages sent via a chat server
 */
public class ChatBot {

    private HashMap<String, String> responseMap; // A map of received messages to the according ChatBot response

    private String username; // The username of this client

    private Socket server; // The socket used to communicate with the server
    private BufferedReader serverIn; // For reading input from the server socket
    private PrintWriter serverOut; // For writing to the server

    private Thread consoleListener; // Thread to listen to and process the console input
    private BufferedReader userIn; // For reading console input

    /**
     * Constructor for ChatBot, sets up variables and defines the thread's run method
     * @param address The address of the server
     * @param port The port to connect to the server on
     */
    public ChatBot(String address, int port) {
        username = "ChatBot";
        responseMap = new HashMap<>();
        fillResponseMap(); // Fills the response map with messages and their responses

        // Sets up connection with server and sets up communication streams
        try {
            server = new Socket(address, port);
            serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));
            serverOut = new PrintWriter(server.getOutputStream(), true);
            System.out.println("Connected to address: " + address);
            System.out.println("Running on port: " + port);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Failed to connect");
            closeResources();
            System.exit(0);
        }

        // Sets up reader for console input
        userIn = new BufferedReader(new InputStreamReader(System.in));

        // Thread for reading user input
        consoleListener = new Thread() {
            public void run() {
                String userInput;

                try {
                    while (true) {
                        // Sleeps to prevent blocking, allowing thread to be interrupted
                        while (!userIn.ready()) {
                            sleep(50);
                        }
                        userInput = userIn.readLine(); // Reads console line
                        if (userInput.equals("LEAVE")) {
                            // If user wants to leave the chat, it is sent to server
                            serverOut.println(userInput);
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Connection lost");
                } catch (InterruptedException ignore) {
                } finally {
                    closeResources();
                }
                System.exit(0);
            }
        };
    }

    /**
     * Sends username to server, starts the console-listening thread, and responds to other client's messages
     */
    public void go() {
        String message;
        String botRes;

        serverOut.println(username); // Sends username to server
        consoleListener.start(); // Starts thread to check for LEAVE command

        try {
            while (true) {
            message = serverIn.readLine(); // Reads message from server

            if (message == null) {
                System.out.println("Disconnected from server");
                break;
            } else {
                if (message.length() > 0 && !message.startsWith("[") && !message.startsWith("(")) {
                    // If messages comes directly from server (not a client), it is outputted to console
                    System.out.println(message);
                } else {
                    // If messages is from client, the appropriate response, if any, is sent
                    botRes = getResponse(message);
                    if (botRes != null) {
                        serverOut.println(botRes);
                    }
                }
            }
            Thread.sleep(50);
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server");
        } catch (InterruptedException ignore) {
        } finally {
            closeResources();
            consoleListener.interrupt();
        }
    }

    /**
     * Takes a message from another client and returns the appropriate response
     * @param message The message received from a client
     * @return The response to send back. Equals null if no response is needed
     */
    private String getResponse(String message) {
        String response = null;
        boolean whisper = message.startsWith("("); // True if the message received was a whisper

        String senderUsername = message.substring(1, message.indexOf(' ') - 1); // Username of sender
        message = message.substring(message.indexOf(' ') + 1); // Actual message sent

        if (!senderUsername.equals(username)) { // Checks message wasn't sent by this chat bot
            if (whisper) {
                // If the message was a whisper
                response = responseMap.get(message.toLowerCase()); // Gets the appropriate response
                if (response != null) {
                    response = "@" + senderUsername + " " + response; // If a response exists, it whispers it back to the sender
                }
            } else if ((message.length() > username.length()) && (message.toLowerCase().startsWith(username.toLowerCase() + " "))) {
                // If the message begins with the username of this chat bot (in upper or lower case)...
                message = message.substring(username.length() + 1); // Removes this chat bot's username (and the following space) from the front of the message
                response = responseMap.get(message.toLowerCase()); // Gets the appropriate response
            }
        }

        return response;
    }

    /**
     * Fills the response map with possible messages and their appropriate response
     */
    private void fillResponseMap() {
        Date date = new java.util.Date();

        responseMap.put("hello", "Hi!");
        responseMap.put("hi", "Hi there!");
        responseMap.put("hey", "Hey there!");
        responseMap.put("hello there", "General Kenobi!");
        responseMap.put("how are you?", "Good thanks!");
        responseMap.put("tell me a joke", "Why did the the programmer quit his job? Because he didn't get arrays.");
        responseMap.put("when were you born?", String.valueOf(date));
        responseMap.put("tell me a dog fact", "Did you know dogs have 3 eyelids?");
        responseMap.put("what's your name?", "My name is " + username + ".");
        responseMap.put("go away", "LEAVE");
    }

    /**
     * Closes all resources used by the chat bot
     */
    private void closeResources() {
        try {
            if (userIn != null) {
                userIn.close();
            }
            if (serverIn != null) {
                serverIn.close();
            }
            if (serverOut != null) {
                serverOut.close();
            }
            synchronized (this) {
                if (server != null && !server.isClosed()) {
                    server.close();
                }
            }
        } catch (IOException e) {
            System.out.println("Closing error");
        }
    }

    public static void main(String[] args) {
        // Sets the address and port based on the inputted arguments
        String address = "localhost";
        int port = 14001;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-cca") && i < args.length - 1) {
                address = args[i + 1];
            } else if (args[i].equals("-ccp") && i < args.length - 1) {
                try {
                    port = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port number format");
                }
            }
        }

        new ChatBot(address, port).go(); // Creates and starts a new chat bot
    }
}