/**
 * Class to create and manage a human player
 */
public class HumanPlayer extends Player {

    private int gold; // The amount of gold the player has

    private String username; // Username of the chat client controlling the player

    /**
     * Constructor for HumanPlayer
     * @param playerChar The character to represent the player on the map
     * @param username The username of the chat client controlling the player
     */
    public HumanPlayer(char playerChar, String username) {
        super(playerChar);
        this.username = username;
        gold = 0;
    }

    /**
     * Gets the amount of gold the player currently has
     * @return The quantity of gold the player has
     */
    public int getGold() {
        return gold;
    }

    /**
     * Increments the amount of gold the player has by 1
     */
    public void pickupGold() {
        gold ++;
    }

    public String getUsername() {
        return username;
    }
}