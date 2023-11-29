import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Class to create a chat server which receives messages from chat clients and forwards them to all clients
 */
public class ChatServer {

	private ServerSocket in; // ServerSocket to accept and connect to clients
	private ArrayList<ChatClientHandler> clientHandlers; // Contains all client handler threads, each managing their own client

	private Thread consoleListener; // Thread which listens to the console for an EXIT command
	private BufferedReader userIn; // For reading console input

	/**
	 * Constructor for ChatServer
	 * @param port The port used in the ServerSocket to connect to clients on
	 */
	public ChatServer(int port) {
		// Sets up ServerSocket, incrementing the port number up to 4 times until the port is valid
		for (int i = 0; i < 5; i++) {
			try {
				in = new ServerSocket(port + i);
				System.out.println("Server running on port: " + (port + i));
				break;
			} catch (IOException | IllegalArgumentException e) {
				if (i == 4) {
					System.out.println("Server setup failed");
					closeResources();
					System.exit(0);
				}
			}
		}
		clientHandlers = new ArrayList<>();

		userIn = new BufferedReader(new InputStreamReader(System.in)); // Setting up console input stream reader

		// Defining run method for thread to read console input
		consoleListener = new Thread() {
			public void run() {
				String userInput;
				try {
					// Repeats until the user types EXIT
					do {
						// Sleeps until reader is ready to prevent blocking, so it can be interrupted
						while (!userIn.ready()) {
							sleep(50);
						}
						userInput = userIn.readLine();
					} while (!userInput.equals("EXIT"));
				} catch (IOException e) {
					System.out.println("An input error occurred");
				} catch (InterruptedException ignore) {
				} finally {
					closeResources();
				}
			}
		};
	}

	/**
	 * Starts up console-listening thread, and repeatedly accepts new clients, creating client handlers to manage each one
	 */
	public void go() {
		System.out.println("Server listening");
		consoleListener.start(); // Begins thread for reading user input

		try {
			// One loop for every client accepted
			while (true) {
				Socket s = in.accept(); // Accepts new client connection
				System.out.println("Connection accepted on " + in.getLocalPort() + " ; " + s.getPort());

				// Sets up input and output streams
				BufferedReader clientIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
				PrintWriter clientOut = new PrintWriter(s.getOutputStream(), true);

				// Creates new client handler and adds it to the ArrayList of client handlers
				ChatClientHandler clientHandler = new ChatClientHandler(s, clientIn, clientOut, clientHandlers, this);
				// Synchronised to prevent simultaneous access and modification of clientHandlers ArrayList
				synchronized (this) {
					clientHandlers.add(clientHandler);
				}
				clientHandler.start();
			}
		} catch (IOException ignore) {
		} finally {
			closeResources();
			consoleListener.interrupt();
			System.out.println("Server shutting down");
		}
	}

	/**
	 * Closes all resources used by the server
	 */
	private void closeResources() {
		try {
			if (userIn != null) {
				userIn.close();
			}
			synchronized (this) {
				if (in != null && !in.isClosed()) {
					in.close();
				}
			}
			// Synchronised to prevent simultaneous access and modification of clientHandlers ArrayList
			synchronized (this) {
				// Closes all client handler threads
				for (ChatClientHandler c : clientHandlers) {
					c.close();
				}
			}
		} catch (IOException e) {
			System.out.println("Closing error");
		}
	}

	public static void main(String[] args) {
		// Sets the port number based on the inputted arguments
		int port = 14001;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-csp") && i < args.length - 1) {
				try {
					port = Integer.parseInt(args[i + 1]);
				} catch (NumberFormatException e) {
					System.out.println("Invalid port number format");
				}
			}
		}
		new ChatServer(port).go(); // Creates and runs a new server
	}
}
