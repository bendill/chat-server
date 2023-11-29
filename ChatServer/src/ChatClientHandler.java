import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Class to manage input from a specific client, and coordinate a response
 */
public class ChatClientHandler implements Runnable {

    private Socket s; // Socket to communicate with a client
    private BufferedReader clientIn; // For reading the input stream from the client
    private PrintWriter clientOut; // For writing to the client via the socket
    private String username; // The username of the client

    private ArrayList<ChatClientHandler> clientHandlers; // Contains client handlers for all clients
    private final ChatServer server; // The server to synchronise on

    private Thread clientHandlerThread; // Thread for handling the client

    /**
     * Constructor for ChatClientHandler
     * @param s The socket used to communicate with the client
     * @param clientIn For reading the input stream from the client
     * @param clientOut For writing to the client via the socket
     * @param clientHandlers // Contains client handlers for all clients
     * @param server // The server to synchronise on
     */
    public ChatClientHandler(Socket s, BufferedReader clientIn, PrintWriter clientOut, ArrayList<ChatClientHandler> clientHandlers, ChatServer server) {
        this.s = s;
        this.clientIn = clientIn;
        this.clientOut = clientOut;
        this.clientHandlers = clientHandlers;
        this.server = server;
        clientHandlerThread = new Thread(this);
    }

    /**
     * Starts the client handler thread
     */
    public void start() {
        clientHandlerThread.start();
    }

    /**
     * Reads the client's username, then repeatedly reads client messages, and forward them to all clients in the chat
     */
    public void run() {
        String message;

        try {
            username = clientIn.readLine(); // The first message read from the client is the username
            if (usernameTaken(username)) {
                sendMessage("Username taken");
            } else {
                sendMessageToAll(username + " has entered the chat");
                while (true) {
                    message = clientIn.readLine();
                    if (message == null || message.equals("LEAVE")) {
                        // If user wants to leave, the loop is broken, and the connection is eventually lost
                        break;
                    } else {
                        forwardMessage(message); // Message sent to all clients in the chat
                    }
                }
            }
        } catch (IOException ignore) {
        } finally {
            sendMessageToAll(username + " has left the chat");
            System.out.println(username + " disconnected");
            // Synchronised on server to prevent simultaneous access and modification of clientHandlers ArrayList
            synchronized (server) {
                clientHandlers.remove(this);
            }
            try {
                clientIn.close();
                clientOut.close();
                synchronized (this) {
                    if (!s.isClosed()) {
                        s.close();
                    }
                }
            } catch (IOException e) {
                System.out.println("Disconnection error");
            }
        }
    }

    /**
     * Determines whether or not an inputted username is currently being used by another client in chat
     * @param testUsername The proposed username
     * @return True if the username is already taken
     */
    private boolean usernameTaken(String testUsername) {
        boolean taken = false;

        // Synchronised on server to prevent simultaneous access and modification of clientHandlers ArrayList
        synchronized (server) {
            // Goes through all other clients to see if they have the same username
            for (ChatClientHandler c : clientHandlers) {
                if (c.getUsername() != null && c.getUsername().equals(testUsername) && c != this) {
                    taken = true;
                    break;
                }
            }
        }
        return taken;
    }

    /**
     * Sends the inputted message to its intended recipient(s)
     * @param message The message to be sent
     */
    private void forwardMessage(String message) {
        if (message.length() > 0 && message.startsWith("@")) {
            // If message is a whisper, it goes through all client handlers and tries to find a client with a matching username
            // Synchronised on server to prevent simultaneous access and modification of clientHandlers ArrayList
            synchronized (server) {
                for (ChatClientHandler c : clientHandlers) {
                    if ((message.length() > c.getUsername().length() + 1) && (message.substring(1, c.getUsername().length() + 2).equals(c.getUsername() + " "))) {
                        // If recipient username matches the client handler username, the message is sent to just that client
                        c.sendMessage("(" + username + ") " + message.substring(c.getUsername().length() + 2));
                        return;
                    }
                }
            }
        }
        // If message wasn't whisper, or the recipient username wasn't found, it is sent to all chat clients
        sendMessageToAll("[" + username + "] " + message);
    }

    /**
     * Closes the socket used by this client handler, causing the thread loop to break
     */
    public void close() {
        try {
            synchronized (this) {
                if (!s.isClosed()) {
                    s.close();
                }
            }
        } catch (IOException e) {
            System.out.println("Disconnection error");
        }
    }

    /**
     * Sends a message to this client handler's client
     * @param message The message to be sent
     */
    public void sendMessage(String message) {
        clientOut.println(message);
    }

    /**
     * Sends a message to all clients in the chat
     * @param message The message to be sent
     */
    public void sendMessageToAll(String message) {
        // Synchronised on server to prevent simultaneous access and modification of clientHandlers ArrayList
        synchronized (server) {
            for (ChatClientHandler c : clientHandlers) {
                c.sendMessage(message);
            }
        }
    }

    public String getUsername() {
        return username;
    }
}