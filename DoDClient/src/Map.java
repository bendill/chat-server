import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

/**
 * Class to create and manage a game map
 */
public class Map {

    private char[][] map; // Contains the map layout itself

    private String mapName; // The name of the map
    private int goldRequired; // The gold required for a player to win the game

    private final int mapWidth; // The width of the map
    private final int mapHeight; // The height of the map

    private final char[] validChars; // Contains all characters which are allowed to be put in the map

    /**
     * The constructor for Map. It creates the map from a specified file.
     * @param filePath The path of the file to read the map from
     */
    public Map(String filePath) {
        validChars = new char[]{'.', '#', 'G', 'E'};
        mapHeight = getFileLength(filePath) - 2; // Sets map height by counting lines. First two lines aren't included in map.
        if (mapHeight >= 0) { // Initialises the map if the file has enough lines
            map = new char[mapHeight][];
        } else { // Outputs error if not enough lines
            invalidFileFormat();
        }
        readMapFile(filePath); // Reads the map file to set map data
        mapWidth = map[0].length;
    }

    /**
     * Finds how many lines are in the given file
     * @param filePath The path of the file to count
     * @return The number of lines counted
     */
    private int getFileLength(String filePath) {
        int lineCount = 0;

        try {
            // Gets the number of lines
            Path path = Paths.get(filePath);
            lineCount = (int) Files.lines(path).count();
        } catch (IOException e) {
            System.out.println(e.toString()); // Outputs error message
            System.exit(0); // Quits program
        }

        return lineCount;
    }

    /**
     * Outputs a specific error message and quits the program
     */
    private void invalidFileFormat() {
        System.out.println("Invalid file format");
        System.exit(0);
    }

    /**
     * Finds if a given character is in a given character array
     * @param testChar The character which is being tested for being in the array
     * @param charArray The array which is being tested for containing the character
     * @return True if the character is in the array, false if not
     */
    private boolean charInArray(char testChar, char[] charArray) {
        // Searches through every character in the array
        for (char c : charArray) {
            if (testChar == c) {
                return true; // If the character is found, returns true
            }
        }

        return false; // Returns false if not already returned
    }

    /**
     * Corrects any invalid characters in the line
     * @param line A line of characters to be corrected
     * @return The corrected line of characters
     */
    private char[] correctChars(char[] line) {
        // Goes through every item in the array
        for (int pos = 0; pos < line.length; pos++) {
            if (!charInArray(line[pos], validChars)) { // If the character isn't valid, it's set to the default character
                line[pos] = '.';
            }
        }
        return line;
    }

    /**
     * Takes a line of text and a header, and returns the data according to the specified format of the map file
     * @param line The line of text to extract the data from
     * @param header The title/name of the data being extracted, which precedes the data in the specified map format
     * @return The data which is extracted from the line
     */
    private String extractDataFromLine(String line, String header) {
        int headerLength = header.length();
        String data = "";

        // Checks that the line is long enough, then checks that the header matches the first word of the line
        if ((line.length() > headerLength + 1) && (line.substring(0, headerLength + 1).equals(header + " "))) {
            data = line.substring(headerLength + 1); // Gets the data following the header
        } else {
            invalidFileFormat(); // Causes error if these conditions aren't met
        }

        return data;
    }

    /**
     * Sets the data of the map object by reading the file
     * @param br A BufferedReader used to read the file. Assumed to be ready to use.
     */
    private void setMapData(BufferedReader br) {
        String line;
        int lineCount = 0;
        int previousLineLength = 0;

        try {
            mapName = extractDataFromLine(br.readLine(), "name"); // Sets map name from first line
            try { // Tries to set goldRequired from second line
                goldRequired = Integer.parseInt(extractDataFromLine(br.readLine(), "win"));
            } catch (NumberFormatException e) {
                invalidFileFormat(); // Causes error if gold value not numeric
            }
            while ((line = br.readLine()) != null) { // Reads until no lines left
                if (line.length() != previousLineLength && lineCount != 0) { // Checks that the current line is the same length as the last one
                    invalidFileFormat();
                }
                map[lineCount] = correctChars(line.toCharArray()); // Sets the map line after converting to array and correcting invalid chars
                lineCount++;
                previousLineLength = line.length();
            }
        } catch (IOException e) {
            System.out.println(e.toString()); // Prints error message
            System.exit(0); // Exits the program
        }
    }

    /**
     * Reads the specified map file and sets the map data accordingly
     * @param filePath The file to read the map data from
     */
    private void readMapFile(String filePath) {
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filePath)); // Sets up BufferedReader
            setMapData(br); // Sets map data using BufferedReader
        } catch (FileNotFoundException e) {
            System.out.println(e.toString()); // Prints error message
            System.exit(0); // Exits program
        } finally {
            try {
                if (br != null) {
                    br.close(); // Closes BufferedReader if not null
                }
            } catch (IOException e) { // If error when closing BufferedReader
                System.out.println(e.toString()); // Outputs error message
                System.exit(0); // Exits program
            }
        }
    }

    /**
     * Gets the character at the specified position
     * @param xPos The horizontal coordinate of the character
     * @param yPos The vertical coordinate of the character
     * @return The character at the position
     */
    public char getCharAtPos(int xPos, int yPos) {
        if (xPos < 0 || yPos < 0 || xPos >= mapWidth || yPos >= mapHeight) {
            return '#'; // Returns a '#' if a coordinate is out of range
        } else {
            return map[yPos][xPos]; // The character at the position
        }
    }

    /**
     * Resets the character at the specified position to the default character
     * @param xPos The horizontal coordinate of the character
     * @param yPos The vertical coordinate of the character
     */
    public void resetCharAtPos(int xPos, int yPos) {
        if (xPos >= 0 && yPos >= 0 && xPos < mapWidth && yPos < mapHeight) { // Checks coordinates are in range
            map[yPos][xPos] = '.';
        }
    }

    /**
     * Gets a grid view of the map with a specified width at a specified position
     * @param xPos The horizontal coordinate of the centre of the grid
     * @param yPos The vertical coordinate of the centre of the grid
     * @param viewWidth The width of the grid returned
     * @param players An array of all the players in the game to be displayed on the map. Players nearer the start of the array get higher priority if overlapping
     * @return The grid view at the position
     */
    public char[][] getMapViewAtPos(int xPos, int yPos, int viewWidth, ArrayList<Player> players) {
        char[][] mapView = new char[viewWidth][viewWidth];
        int viewRadius = Math.max((viewWidth - 1) / 2, 0); // Radius calculated from the width
        int tempXPos, tempYPos;

        // Goes through each position in the created grid and sets its character
        for (int y = 0; y < viewWidth; y++) {
            for (int x = 0; x < viewWidth; x++) {
                tempXPos = xPos - viewRadius + x; // x coordinate on the the actual map
                tempYPos = yPos - viewRadius + y; // y coordinate on the the actual map
                mapView[y][x] = getCharAtPos(tempXPos, tempYPos);
                // Goes through each player and puts their characters on the grid
                for (Player p : players) {
                    if (p.getXPos() == tempXPos && p.getYPos() == tempYPos) {
                        mapView[y][x] = p.getPlayerChar();
                        break; // As soon as a player is found in the position, it doesn't check other players
                    }
                }
            }
        }

        return mapView;
    }

    /**
     * Places more gold on the map randomly
     * @param quantity The number of gold squares to add
     * @param players The list of players in the game
     */
    public void placeExtraGold(int quantity, ArrayList<Player> players) {
        int xPos, yPos;
        boolean valid;
        final int attemptLimit = 1000; // Maximum number of attempts for placing each player
        Random random = new Random();

        for (int i = 0; i < quantity; i++) { // One loop for each gold being placed
            valid = true;
            for (int attempt = 0; attempt < attemptLimit; attempt++) { // Tries a certain amount of times before giving up
                // Sets random coordinates on the map
                xPos = random.nextInt(mapWidth);
                yPos = random.nextInt(mapHeight);
                // Tests that the gold won't be placed on any players
                for (Player p : players) {
                    if (xPos == p.getXPos() && yPos == p.getYPos()) {
                        valid = false; // Position not valid if clash found
                        break;
                    }
                }
                // Tests that the position isn't in a wall or on gold
                if (getCharAtPos(xPos, yPos) == '#' || getCharAtPos(xPos, yPos) == 'G') {
                    valid = false; // Position not valid if on wall or gold
                }
                if (valid) {
                    map[yPos][xPos] = 'G';
                    break;
                }
            }
        }
    }

    /**
     * Gets the gold required to win the game
     * @return The gold required to win the game
     */
    public int getGoldRequired() {
        return goldRequired;
    }

    /**
     * Gets the width of the map
     * @return The map width
     */
    public int getMapWidth() {
        return mapWidth;
    }

    /**
     * Gets the height of the map
     * @return The map height
     */
    public int getMapHeight() {
        return mapHeight;
    }

    /**
     * Gets the name of the map
     * @return The map name
     */
    public String getMapName() {
        return mapName;
    }
}
