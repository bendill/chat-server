import java.util.Random;

/**
 * Class to create and manage computer-controlled player
 */
public class BotPlayer extends Player {

    private int knownOpponentXDist; // The last seen x position of the human player, relative to the bot's position
    private int knownOpponentYDist; // The last seen y position of the human player, relative to the bot's position
    private boolean searching; // True if the bot is searching for the player's position, rather than chasing
    private int searchCounter; // Coordinates the timings of commands when searching

    private final char[] directions; // An array of directions to use in commands

    /**
     * The constructor for BotPlayer
     * @param playerChar The character to represent the player on the map
     */
    public BotPlayer(char playerChar) {
        super(playerChar);
        knownOpponentXDist = 0;
        knownOpponentYDist = 0;
        searching = true;
        searchCounter = 0;
        directions = new char[]{'N', 'S', 'E', 'W'};
    }

    /**
     * Selects a command to try and find the player's position
     * @return The selected command
     */
    private String getSearchCommand() {
        Random random = new Random();
        if (searchCounter % 4 == 0) { // Every 4 loops, the bot looks for the player
            return "LOOK";
        } else { // Every other loop, the bot moves in a random direction
            return "MOVE " + directions[random.nextInt(4)];
        }
    }

    /**
     * Selects a command to move to the player's last known position
     * @return The selected command
     */
    private String getChaseCommand() {
        if (knownOpponentXDist < 0) { // Moves west if the player's known position is to the left
            knownOpponentXDist++; // Adjusts the relative opponent position
            return "MOVE W";
        } else if (knownOpponentXDist > 0) { // Moves west if the player's known position is to the right
            knownOpponentXDist--; // Adjusts the relative opponent position
            return "MOVE E";
        } else if (knownOpponentYDist < 0) { // Moves north if the player's known position is above the bot
            knownOpponentYDist++; // Adjusts the relative opponent position
            return "MOVE N";
        } else if (knownOpponentYDist > 0) { // Moves south if the player's known position is below the bot
            knownOpponentYDist--; // Adjusts the relative opponent position
            return "MOVE S";
        } else { // If bot reaches the last seen player position, it starts searching again
            searching = true;
            searchCounter = 1;
            return "LOOK";
        }
    }

    /**
     * Selects a command for the bot
     * @return The selected command
     */
    public String[] getCommand() {
        String command;

        if (searching) { // If the bot doesn't know the player's position, it searches for the player
            command = getSearchCommand();
            searchCounter++;
        } else { // If the bot has seen the player, it moves to the position it was seen at
            command = getChaseCommand();
        }
        command = command.toUpperCase(); // Command converted to upper case

        return command.split(" "); // Command split into an array, separated by spaces
    }

    /**
     * Processes the data returned by the 'look' command to find the player
     * @param mapView The grid view produced by the command
     * @param opponentPlayerChar The opponent's player char to search for
     */
    public void processLookResult(char[][] mapView, char opponentPlayerChar) {
        // Goes through every item in the grid and searches for the human player's character
        for (int y = 0; y < mapView.length; y++) {
            for (int x = 0; x < mapView[0].length; x++) {
                if (mapView[y][x] == opponentPlayerChar) {
                    // Calculates the player's position relative to the bot, using the index of the player's character in the grid
                    knownOpponentXDist = x - (mapView[0].length - 1) / 2;
                    knownOpponentYDist = y - (mapView.length - 1) / 2;
                    searching = false; // Bot now chases down the player
                }
            }
        }
    }
}