import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

/**
 * Class to create and control a game of Dungeons of Doom as a client connected to a server
 */
public class DoDClient {

    private Map map; // The game map

    private BotPlayer botPlayer; // The computer-controlled player playing the game
    private final char humanPlayerChar = 'P'; // The character used to represent human players on the map
    private ArrayList<Player> players; // Contains all players in the game

    private String username; // The username of the DoDClient - used when communicating with server
    private Socket server; // Socket used to communicate with server
    private BufferedReader serverIn; // To read incoming messages from the server
    private PrintWriter serverOut; // To output messages to the server

    private BufferedReader userIn; // To read messages coming from the console
    private Thread consoleListener; // Thread used to process user input from console

    /**
     * The constructor for DoDClient
     * @param address The address of the server to connect to
     * @param port The port used to communicate with the server
     */
    public DoDClient(String address, int port) {
        // Initialises the map, bot player and players ArrayList
        map = new Map("map.txt");
        botPlayer = new BotPlayer('B');
        players = new ArrayList<>();

        // Establishes connection with server, and sets up server input and output streams
        username = "DoDClient";
        try {
            server = new Socket(address, port);
            System.out.println("Connected to address: " + address);
            System.out.println("Running on port: " + port);
            serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));
            serverOut = new PrintWriter(server.getOutputStream(), true);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Failed to connect");
            closeResources();
            System.exit(0);
        }

        userIn = new BufferedReader(new InputStreamReader(System.in)); // For reading user console input

        consoleListener = new Thread() {
            public void run() {
                String userInput;

                try {
                    while (true) {
                        // Sleeps to prevent blocking on BufferedReader
                        while (!userIn.ready()) {
                            sleep(50);
                        }
                        userInput = userIn.readLine();
                        // If user wants to leave chat, it is sent to server
                        if (userInput.equals("LEAVE")) {
                            // Synchronised to prevent two threads sending data to the same stream at the same time
                            synchronized (this) {
                                serverOut.println(userInput);
                            }
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
     * Carries out the 'hello' command
     * Gets the gold required to win the game
     * @return A message stating gold required to win the game
     */
    private String hello() {
        return "Gold to win: " + map.getGoldRequired();
    }

    /**
     * Carries out the 'gold' command
     * Gets the amount of gold the inputted player currently has
     * @param player The player who gave the command
     * @return A message stating the amount of gold the inputted player has
     */
    private String gold(HumanPlayer player) {
        return "Gold owned: " + player.getGold();
    }

    /**
     * Carries out the 'move' command
     * Detects if the move is valid, and moves the player if so
     * @param player The player being moved
     * @param direction The direction of movement (N, E, S, W)
     * @return A message stating whether or not the move was successful
     */
    private String move(Player player, String direction) {
        int xMove = 0;
        int yMove = 0;

        // Translates the direction to the coordinate change
        switch (direction) {
            case "N":
                yMove = -1;
                break;
            case "S":
                yMove = 1;
                break;
            case "E":
                xMove = 1;
                break;
            case "W":
                xMove = -1;
                break;
        }
        if (xMove == 0 && yMove == 0) { // Command fails if direction isn't valid
            return "FAIL";
        } else {
            // If the player is trying to move into a wall ('#'), the command fails. If not, the player is moved successfully
            if (map.getCharAtPos(player.getXPos() + xMove, player.getYPos() + yMove) != '#') {
                player.move(xMove, yMove);
                return "SUCCESS";
            } else {
                return "FAIL";
            }
        }
    }

    /**
     * Carries out the 'pickup' command
     * Checks that the inputted player is on top of gold, and lets them pick it up if so
     * @param player The player who gave the pickup command
     * @return A message stating the result of the command, and the amount of gold the player now has
     */
    private String pickup(HumanPlayer player) {
        String outputMessage;

        // If the player's position contains gold, give the player gold, and remove the gold from the map
        if (map.getCharAtPos(player.getXPos(), player.getYPos()) == 'G') {
            player.pickupGold();
            map.resetCharAtPos(player.getXPos(), player.getYPos());
            outputMessage = "SUCCESS";
        } else {
            outputMessage = "FAIL"; // If player not on gold, command fails
        }

        return outputMessage + ". Gold owned: " + player.getGold();
    }

    /**
     * Carries out the 'look' command
     * Gets a 5x5 grid view of the map around the player and translates it to a printable string
     * @param player The player who is looking
     * @return The string representation of the grid
     */
    private String look(Player player) {
        final int viewWidth = 5; // Specifies the width of the grid
        // Gets grid view from map
        char[][] mapView = map.getMapViewAtPos(player.getXPos(), player.getYPos(), viewWidth, players);

        if (player.getClass().equals(BotPlayer.class)) { // If the player is a bot, it lets the bot process the data
            ((BotPlayer) player).processLookResult(mapView, humanPlayerChar);
        }
        // Converts the 2D char array to a printable string
        StringBuilder mapViewOutput = new StringBuilder();
        for (int y = 0; y < viewWidth; y++) {
            for (int x = 0; x < viewWidth; x++) {
                mapViewOutput.append(mapView[y][x]); // Adds each char to the string
            }
            if (y < viewWidth - 1) {
                mapViewOutput.append("\n"); // Adds a new line at the end of each row, unless it is the last row
            }
        }

        return mapViewOutput.toString();
    }

    /**
     * Carries out the 'quit' command
     * If the player is on an exit and has enough gold to win, they win
     * If not, they lose
     * @param player who gave the quit command
     * @return A message stating whether they have won or not
     */
    private String quit(HumanPlayer player) {
        String response;

        // If player is on exit and has enough gold
        if (map.getCharAtPos(player.getXPos(), player.getYPos()) == 'E' && player.getGold() >= map.getGoldRequired()) {
            response = "WIN"; // Result of command, which is later sent to player
            // Tells the chat that the player has won. Synchronised to stop consoleListener sending at the same time
            synchronized (this) {
                serverOut.println("Player " + player.getUsername() + " has won. Congratulations!");
            }
        } else {
            response = "LOSE"; // Result of command, which is later sent to player
            // Tells the chat which player lost the game. Synchronised to stop consoleListener sending at the same time
            synchronized (this) {
                serverOut.println("Player " + player.getUsername() + " has lost.");
            }
        }
        players.remove(player); // Removes player from game

        return response;
    }

    /**
     * Gives the inputted player a random location on the map
     * @param player The player being placed
     * @return True if the player was successfully placed, false if not
     */
    private boolean setPlayerPosition(Player player) {
        int xPos = 0;
        int yPos = 0;
        boolean valid = false; // Whether or not the current placement is valid
        int repeatCounter = 0; // Counts the number of attempts
        final int repeatLimit = 1000; // Maximum number of attempts for placing each player
        Random random = new Random();

        while (!valid) { // Repeats until a placement is valid
            // If repeat counter reaches limit, error message outputted and false returned
            if (repeatCounter >= repeatLimit) {
                System.out.println("Player can't be placed");
                return false;
            }
            valid = true;
            // Sets random coordinates on the map
            xPos = random.nextInt(map.getMapWidth());
            yPos = random.nextInt(map.getMapHeight());
            // Tests that the coordinates don't clash with any other player's
            for (Player otherPlayer : players) {
                if (player != otherPlayer && xPos == otherPlayer.getXPos() && yPos == otherPlayer.getYPos()) {
                    valid = false; // Position not valid if clash found
                    break;
                }
            }
            // Tests that the position isn't on a wall or on gold
            if (map.getCharAtPos(xPos, yPos) == '#' || map.getCharAtPos(xPos, yPos) == 'G') {
                valid = false; // Position not valid if on wall or gold
            }
            repeatCounter++;
        }
        player.setPos(xPos, yPos); // Finally sets valid position of player
        return true;
    }

    /**
     * Checks if the bot player has caught any players
     * @return An ArrayList of players who have lost
     */
    private ArrayList<HumanPlayer> checkBotCollisions() {
        ArrayList<HumanPlayer> playersCaught = new ArrayList<>();
        // Checks if the bot player is at the same position as any other players
        for (Player p: players) {
            if (p.getXPos() == botPlayer.getXPos() && p.getYPos() == botPlayer.getYPos() && p.getClass().equals(HumanPlayer.class)) {
                playersCaught.add((HumanPlayer) p);
            }
        }
        return playersCaught;
    }

    /**
     * Removes any players who are on the same space as the bot
     */
    private void removeCaughtPlayers() {
        // Goes through ArrayList of players who are caught
        for (HumanPlayer p : checkBotCollisions()) {
            // Tells the chat that the current player has been caught. Synchronised to stop consoleListener sending at the same time
            synchronized (this) {
                serverOut.println("Player " + p.getUsername() + " has lost. The bot caught you"); // Prints losing message
            }
            // Removes player from game
            players.remove(p);
        }
    }

    /**
     * Activates the command method according to the specified command
     * Gets the result of the command as a string
     * @param command An array containing the command being activated - split by spaces
     * @param player The player who gave the command
     * @return The result of the command which can be outputted, null if command is invalid
     */
    private String executeCommand(String[] command, Player player) {
        String commandOutput;
        String commandWord = command[0];

        // Calls the appropriate method, according to the command given
        if (command.length == 1) {
            if (commandWord.equals("HELLO")) {
                commandOutput = hello();
            } else if (commandWord.equals("GOLD") && player.getClass().equals(HumanPlayer.class)) { // Only human players can give this command
                commandOutput = gold((HumanPlayer) player);
            } else if (commandWord.equals("PICKUP") && player.getClass().equals(HumanPlayer.class)) { // Only human players can give this command
                commandOutput = pickup((HumanPlayer) player);
            } else if (commandWord.equals("LOOK")) {
                commandOutput = look(player);
            } else if (commandWord.equals("QUIT") && player.getClass().equals(HumanPlayer.class)) { // Only human players can give this command
                commandOutput = quit((HumanPlayer) player);
            } else {
                commandOutput = null; // If none of the above, the command is invalid
            }
        } else if (command.length == 2 && commandWord.equals("MOVE")) { // The only valid command of length 2
            commandOutput = move(player, command[1]);
        } else {
            commandOutput = null; // If none of the above, the command is invalid
        }

        return commandOutput;
    }

    /**
     * Runs one game of DoD
     * Accepts messages from the server, and executes any valid commands from players
     * Controls the bot and detects player wins / losses
     */
    private void playGame() throws IOException, InterruptedException {
        String message;
        HumanPlayer leavingPlayer;
        int turnCounter = 0;

        while (true) {
            message = serverIn.readLine(); // Reads line from server stream
            if (message == null) {
                throw new IOException();
            } else {
                if (message.length() > 0 && !message.startsWith("[") && !message.startsWith("(")) {
                    // If message doesn't come from a client, outputs it to console
                    System.out.println(message);
                    // If a player who's in the game has left the chat, remove them from the game
                    if (message.endsWith("has left the chat")) {
                        leavingPlayer = getPlayerFromUsername(message.substring(0, message.indexOf(' ')));
                        if (leavingPlayer != null) {
                            players.remove(leavingPlayer);
                        }
                    }
                } else {
                    // Separates the username from the actual message
                    String senderUsername = message.substring(1, message.indexOf(' ') - 1);
                    message = message.substring(message.indexOf(' ') + 1);

                    // Process the message, and execute the command if necessary
                    if (processClientMessage(senderUsername, message)) {
                        // If message was valid command...
                        removeCaughtPlayers(); // Remove any players caught by the bot

                        turnCounter ++;
                        // Give the bot a turn every n turns, where n is the number of human players in the game
                        // The bot will on average get the same number of turns as any other player
                        if (turnCounter >= players.size() - 1) {
                            executeCommand(botPlayer.getCommand(), botPlayer); // Execute the bot's chosen command
                            removeCaughtPlayers(); // Remove any players caught by the bot
                            turnCounter = 0;
                        }
                    }
                }
            }
            Thread.sleep(50);
        }
    }

    /**
     * Assigns initial values of variables needed to set up the game for the first time
     */
    private void setupGame() {
        // Gives the bot a position, if possible
        if (!setPlayerPosition(botPlayer)) {
            closeResources();
            System.exit(0);
        }
        players.add(botPlayer);
    }

    /**
     * Processes a message received from a chat client, and executes the appropriate command
     * @param senderUsername The username of the client who sent the message
     * @param message The message sent by the client
     * @return True if the message was a valid command from a player in the game, false if not
     */
    private boolean processClientMessage(String senderUsername, String message) {
        String[] command;
        String commandResult;

        HumanPlayer senderPlayer = getPlayerFromUsername(senderUsername); // Gets player object from sender username

        message = message.toUpperCase();
        if (message.equals("JOIN")) { // If client wants to join game...
            if (senderPlayer != null) { // If client is already in game...
                // Synchronised to stop consoleListener sending at the same time
                synchronized (this) {
                    serverOut.println("Player " + senderUsername + " already in game");
                }
            } else {
                // Adds player to game
                if (addHumanPlayer(senderUsername)) { // If addition was successful...
                    // Tells chat that a player joined the game. Whispers to player to welcome them. Synchronised to stop consoleListener sending at the same time
                    synchronized (this) {
                        serverOut.println("Player " + senderUsername + " added to game");
                        serverOut.println("@" + senderUsername + " Welcome to the " + map.getMapName());
                    }
                } else {
                    // Synchronised to stop consoleListener sending at the same time
                    synchronized (this) {
                        serverOut.println("Player " + senderUsername + " could not be added to game");
                    }
                }
            }
        } else {
            if (senderPlayer != null) { // If client is in the game
                command = message.toUpperCase().split(" "); // Command split into array of words
                commandResult = executeCommand(command, getPlayerFromUsername(senderUsername)); // Execute command and get string result
                if (commandResult != null) { // If command was valid
                    commandResult = commandResult.replace("\n", "\n@" + senderUsername + " "); // Whisper each line (each line sent individually)
                    // Synchronised to stop consoleListener sending at the same time
                    synchronized (this) {
                        serverOut.println("@" + senderUsername + " " + commandResult);
                    }
                    return true; // Message was valid command from client in the game
                }
            }
        }
        return false; // Message wasn't valid command from client in the game
    }

    /**
     * Adds a new player to the game and places them on map
     * @param playerUsername The username of the client controlling the player
     * @return True if the placement of the player was successful, false if not
     */
    private boolean addHumanPlayer(String playerUsername) {
        boolean success;

        HumanPlayer newPlayer = new HumanPlayer(humanPlayerChar, playerUsername);
        success = setPlayerPosition(newPlayer); // Place player and get result of placements (success / fail)
        if (success) {
            players.add(newPlayer);
            map.placeExtraGold(map.getGoldRequired(), players); // Add extra gold to the map so it is possible for the new player to win
        }
        return success;
    }

    /**
     * Gets the player object with the inputted username
     * @param username The username of the player to return
     * @return The player object with the inputted username, null if none found
     */
    private HumanPlayer getPlayerFromUsername(String username) {
        for (Player p : players) {
            if (p.getClass().equals(HumanPlayer.class) && ((HumanPlayer) p).getUsername().equals(username)) {
                return (HumanPlayer) p;
            }
        }
        return null;
    }

    /**
     * Closes all resources used by DoDClient
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

    /**
     * Starts and runs a new game of Dungeons of Doom
     */
    private void begin() {
        serverOut.println(username); // Gives username to server
        consoleListener.start(); // Runs the thread to check console input
        try {
            setupGame();
            // Tells chat how to join the game. Synchronised to stop consoleListener sending at the same time
            synchronized (this) {
                serverOut.println("Type JOIN to enter the game.");
            }
            playGame();
        } catch (IOException e) {
            System.out.println("Disconnected from server");
        } catch (InterruptedException ignore) {
        } finally {
            closeResources();
            consoleListener.interrupt();
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

        new DoDClient(address, port).begin(); // Creates and begins a new game
    }
}