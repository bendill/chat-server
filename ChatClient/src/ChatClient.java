import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Class to create a chat client who can send and receive messages from a chat server
 */
public class ChatClient {

	private Socket server; // Socket used to connect to server

	private Thread userListener; // Thread for listening to the user (console) and sending messages to server
	private Thread serverListener; // Thread for listening to the server input stream and displaying messages to the user (console)

	private String username; // Username of this client

	private BufferedReader userIn;  // To read the user console input
	private PrintWriter serverOut; // For sending messages to the server
	private BufferedReader serverIn; // For reading messages from the server

	/**
	 * Constructor for ChatClient, sets up variables and defines thread methods
	 * @param address The address of the server
	 * @param port The port used to connect to the server
	 */
	public ChatClient(String address, int port) {
		// Connecting to server and setting up communication streams
		try {
			server = new Socket(address, port);
			serverOut = new PrintWriter(server.getOutputStream(), true);
			serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));
			System.out.println("Connected to address: " + address);
			System.out.println("Running on port: " + port);
		} catch (IOException | IllegalArgumentException e) {
			System.out.println("Failed to connect");
			closeResources();
			System.exit(0);
		}

		userIn = new BufferedReader(new InputStreamReader(System.in)); // Setting up reader to read console input stream
		username = getUsernameInput(); // Gets username input from user

		// Defining thread to read user input and send it to server
		userListener = new Thread() {
			public void run() {
				String userInput;

				try {
					while (true) {
						// Sleeps to prevent blocking, to allow interrupt at any time
						while (!userIn.ready()) {
							sleep(50);
						}
						// Reads line from console and outputs it to server if characters were entered
						userInput = userIn.readLine();
						if (userInput.length() > 0) {
							serverOut.println(userInput);
						}
					}
				} catch (IOException e) {
					System.out.println("Connection lost");
				} catch (InterruptedException ignore) {
				} finally {
					closeResources();
					serverListener.interrupt(); // Stops other thread
				}
			}
		};

		serverListener = new Thread() {
			public void run() {
				String message;

				try {
					while (true) {
						message = serverIn.readLine(); // Reads line from server

						if (message == null) {
							System.out.println("Disconnected from server");
							break;
						} else {
							System.out.println(message); // Prints message to console
						}
						sleep(50);
					}
				} catch (IOException e) {
					System.out.println("Connection lost");
				} catch (InterruptedException ignore) {
				} finally {
					closeResources();
					userListener.interrupt(); // Stops other thread
				}
			}
		};
	}

	/**
	 * Sends username to server and starts client threads
	 */
	public void go() {
		serverOut.println(username); // Sends username to server
		// Starts both threads
		userListener.start();
		serverListener.start();
	}

	/**
	 * Gets input from the user until they enter a valid username
	 * @return The new username for this client
	 */
	private String getUsernameInput() {
		String newUsername = "User"; // Default username
		// Repeats until valid username is entered
		while (true) {
			System.out.print("Enter a username: ");
			try {
				newUsername = userIn.readLine();
			} catch (IOException e) {
				System.out.println("An input error occurred");
			}
			if (newUsername.contains(" ")) {
				System.out.println("No spaces allowed");
			} else if (newUsername.length() > 0) {
				// If username has no spaces and contains characters, it is accepted
				break;
			}
		}
		return newUsername;
	}

	/**
	 * Closes all resources used by the chat client
	 */
	private void closeResources() {
		try {
			if (userIn != null) {
				userIn.close();
			}
			if (serverOut != null) {
				serverOut.close();
			}
			if (serverIn != null) {
				serverIn.close();
			}
			synchronized (this) {
				if (server != null && !server.isClosed()) {
					server.close();
				}
			}
		} catch (IOException e) {
			System.out.println("Disconnection error");
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

		new ChatClient(address, port).go(); // Starts a new client
	}
}
