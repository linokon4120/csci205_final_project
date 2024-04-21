package org.team11.GameController;

import javafx.animation.PathTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.team11.GameView.Ghost;
import org.team11.GameView.WordDictionary;
import org.team11.TypingMechanism.GhostAnimation;
import org.team11.TypingMechanism.GuessStatus;
import org.team11.TypingMechanism.WordsSetting;

import java.io.IOException;
import java.util.*;

public class KeyFrenzyView {
    private VBox root;
    private Label labelMessageBanner;
    private Label currentScore;
    private Label lives;
    private GridPane gamePane;
    private List<Ghost> ghosts;

    private final Map<String, GhostAnimation> wordTimers = new HashMap<>();
    private TextField userTypeBox;
    private final WordDictionary wordDictionary;
    private final Random rand;
    private boolean lost;
    private final Timer globalTimer;
    private final String userName;


    //The width of the game pane
    private double paneWidth;

    //The height of the game pane
    private double paneHeight;
    /**
     * Variable to check the score
     */

    //Variable to check the score
    private int score;

    //Variable for starting lives;
    public static int startingLives = 3;

    public static double centerX;
    public static double centerY;


    /**
     * /**
     * This is the "view" in the MVC design for the game Key Frenzy. A view class
     * does nothing more than initializes all nodes for the scene graph for this view.
     */
    public KeyFrenzyView(String username) {
        this.userName = username;
        score = 0;

        wordDictionary = new WordDictionary();

        lost = false;
        rand = new Random(System.currentTimeMillis());

        initSceneGraph();

        globalTimer = new Timer();
        globalTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                generateNewWord();
            }
        }, 5, WordsSetting.WORD_DELAY); // 5 is the time delayed before the first ghost appears
    }

    /**
     * Initialize the entire scene graph
     */
    public void initSceneGraph() {

        // Initialize the root pane
        this.root = new VBox();

        // Create and configure the game pane
        gamePane = new GridPane();

        AnchorPane bottomPane = new AnchorPane();

        // Set minimum size for the gamePane
        gamePane.setMinSize(800, 600); // Set minimum width
        // TODO Get the paneWidth and paneHeight of the game Pane


        this.gamePane.getStyleClass().add("game-pane"); // Apply CSS class to gamePane

        paneWidth = 800;
        paneHeight = 600;
        // Create and configure the message banner
        configuringMessageBanner();


        // Initialize ghosts
        this.ghosts = new ArrayList<>();

        //Adding the text box to the game
        bottomPane.getChildren().add(userTypeBox);
        // Display the username in the middle of the view
        Text userNameText = new Text(userName);
        userNameText.setStyle("-fx-font-size: 24;");
        userNameText.setStyle("-fx-background-color: WHITE");
        gamePane.add(userNameText, 50, 50);

        this.root.getChildren().add(labelMessageBanner);
        this.root.getChildren().add(currentScore);
        this.root.getChildren().add(lives);
        this.root.getChildren().add(gamePane);
        this.root.getChildren().add(bottomPane);
    }


    //TODO (Holiness) Clean this up and add comments

    /**
     * Adds the message banner into the home screen of the came
     */
    private void configuringMessageBanner() {
        labelMessageBanner = new Label("Type words on ghosts to destroy them!");
        this.labelMessageBanner.getStyleClass().add("instruct-banner");
        currentScore = new Label("Current Score: ");
        this.currentScore.getStyleClass().add("current-score");
        this.userTypeBox = new TextField();
        userTypeBox.getStyleClass().add("user-type-box");
        lives = new Label("Remaining Lives: " + startingLives);
        this.lives.getStyleClass().add("remaining-lives");

        // VBox to contain username and time labels
        VBox userInfoBox = new VBox();
        userInfoBox.getStyleClass().add("user-info-box");

        // Display the username and time used in the corner of the view
        Label usernameLabel = new Label("Username: " + userName);
        usernameLabel.getStyleClass().add("user-nickname");

        Label timeUsedLabel = new Label("Time Used: 00:00");
        timeUsedLabel.getStyleClass().add("time-spent");

        userInfoBox.getChildren().addAll(usernameLabel, timeUsedLabel);

        // Add VBox to message banner
        VBox.setMargin(userInfoBox, new Insets(10)); // Adjust margin as needed
        labelMessageBanner.setGraphic(userInfoBox);

        // Add the labels to the game pane
        currentScore.getStyleClass().add("current-score");
        lives.getStyleClass().add("remaining-lives");
        this.labelMessageBanner.getStyleClass().add("instruct-banner");

        // TODO Modify how we handle the user input to regenerate a new ghost
        userTypeBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                String textInput = userTypeBox.getText().trim().toLowerCase(Locale.ROOT);

                handleUserInput(userTypeBox.getText().trim());
                userTypeBox.clear();

                GuessStatus guessStatus = wordDictionary.guess(textInput);

            }
        });
    }


    /**
     * Handle the user input when prompted
     *
     * @param userInput the String input from user
     */
    private void handleUserInput(String userInput) {

        Iterator<Ghost> iterator = ghosts.iterator();
        while (iterator.hasNext()) {
            Ghost ghost = iterator.next();
            if (ghost.getWord().equalsIgnoreCase(userInput)) {

                // Word matched, remove the ghost from the game pane
                destroy(ghost);
                iterator.remove();

                // Update the score
                score += 10;
                updateScoreLabel();
                break;
            }
        }

    }

    /**
     * Updates the score on the game pane
     */
    private void updateScoreLabel() {
        currentScore.setText("Current Score: " + score);
    }

    /**
     * Updates the lives  on the game pane
     */
    private void updateLivesLabel() {
        lives.setText("Remaining lives: " + startingLives);
    }


    /**
     * Starts the timer and generates a new word to be typed by the player
     */
    private void generateNewWord() {
        // Generate the new word
        String word = wordDictionary.getWord();

        // Create a timer that ends the game if the player does not type the word in time.
        Timer wordTimer = new Timer();

        wordTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // When the timer runs out (8 seconds), that means the player loses
                lost = true;
                gameOver();
            }
        }, WordsSetting.GAME_LENGTH);

        // Add the words to the global map and draws it on the screen
        List<Ghost> ghostsOnScreen = createAnimation(word);
        ghosts.add(ghostsOnScreen.get(0));
        ghosts.add(ghostsOnScreen.get(1));

        //Start Ghost Animations
        startGhostAnimation(ghostsOnScreen.get(0));
        startGhostAnimation(ghostsOnScreen.get(1));

        //Store ghost animation in the map
        storeGhostAnimation(word, wordTimer, ghostsOnScreen, 0);
        storeGhostAnimation(word, wordTimer, ghostsOnScreen, 1);
    }

    /**
     * Stores the ghost animation into the map
     */
    private void storeGhostAnimation(String word, Timer wordTimer, List<Ghost> ghostsOnScreen, int index) {
        wordTimers.put(word, new GhostAnimation(wordTimer, ghostsOnScreen.get(index)));
    }

    /**
     * Starts the ghost animations
     *
     * @param ghost to be animated
     */
    private void startGhostAnimation(Ghost ghost) {
        GhostAnimation animation = wordTimers.get(ghost.getWord());

        if (animation != null) {
            animation.start();
        }
    }

    /**
     * Ties the text on top of the ghost,
     * Runs the Animation on the FX app thread,
     * Moves the ghosts towards the middle of the screen
     * Generates a path and an animation, and adds it to the game pane
     *
     * @param word the string of words to be typed
     * @return text on top of the ghost that was destroyed
     */
    private List<Ghost> createAnimation(String word) {

        // Create the text object
        List<Ghost> ghostsOnScreen = new ArrayList<>();
        String[] words = wordDictionary.getWords(3, 4).toArray(new String[0]);


        long creationTime = System.currentTimeMillis();
        Ghost ghost1 = new Ghost(words[0], 80);
        Ghost ghost2 = new Ghost(words[1], 80);
        //Starts the timer
        ghost1.setCreationTime(creationTime);
        ghost2.setCreationTime(creationTime);
        ghostsOnScreen.add(ghost1);
        ghostsOnScreen.add(ghost2);


        // Run the animation on the FX App thread
        Platform.runLater(() -> {

            // Moves the animation towards the center of the game pane
            // Get x and y coords of the word
            double x1 = paneWidth / 2;
            double y1 = rand.nextDouble() * paneHeight;

            // Get x and y coords of the word
            double x2 = paneWidth;
            double y2 = rand.nextDouble() * paneHeight;

            // Generate a path for ghosts coming from left side
            Path path1 = new Path();
            path1.getElements().add(new MoveTo(-50, y1));

            // Moves the path to the middle of the pane
            moveToCenter(ghost1, path1);

            // Generate a path for ghosts coming from right side
            Path path2 = new Path();
            path2.getElements().add(new MoveTo(x2 + 50, y2));
            // Moves the path to the middle of the pane
            moveToCenter(ghost2, path2);


            // Generate animation
            createPath(path1, ghost1);
            createPath(path2, ghost2);


            // Add to pane
            gamePane.getChildren().add(ghost1.getNode());
            gamePane.getChildren().add(ghost2.getNode());

            //Checks destroy ghosts
            checkDestroyGhosts(ghostsOnScreen);

        });

        return ghostsOnScreen;

    }

    /**
     * Destroys the ghosts based on the starting time of the animation
     *
     * @param ghostsOnScreen that have been animated
     */
    private void checkDestroyGhosts(List<Ghost> ghostsOnScreen) {
        long currentTime = System.currentTimeMillis();
        for (Ghost ghost : ghostsOnScreen) {
            if (currentTime - ghost.getCreationTime() >= WordsSetting.WORD_DELAY) {
                destroy(ghost);
                deductALife();
            }
        }
    }

    /**
     * Removes a life and ends the game if there's no remaining lives.
     */
    private void deductALife() {
        startingLives--;

        updateLivesLabel();

        if (startingLives == 0) {
            gameOver();
        }

    }

    /**
     * Moves the ghosts to the center of the game pane
     *
     * @param path to be moved
     */
    private void moveToCenter(Ghost ghost, Path path) {

        centerX = paneWidth / 2;
        centerY = paneHeight / 2;

        ghost.setPosition(centerX, centerY);
        path.getElements().add(new LineTo(centerX, centerY));


    }


    /**
     * Creates a path to be followed by the ghost
     *
     * @param path  followed by the ghost
     * @param ghost to be destroyed
     */
    private static void createPath(Path path, Ghost ghost) {
        PathTransition pt = new PathTransition();
        pt.setDuration(Duration.millis(WordsSetting.WORD_DURATION));
        pt.setPath(path);
        pt.setNode(ghost.getNode());
        pt.setCycleCount(1);
        pt.play();
    }


    private void gameOver() {
        // Perform actions on the main thread
        Platform.runLater(() -> {
            // Stop all timers
            globalTimer.cancel();
            for (GhostAnimation wa : wordTimers.values()) {
                wa.stop();
            }

            // TODO Switch to Game Over view

            try {
                // Transfer game object to game over controller
                GameOverController newController = SceneSwitch.change(currentScore, "game-over-view.fxml");

                // Move to the game over screen
                newController.transferData(wordDictionary);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Makes the Ghosts disappear from the game pane,
     *
     * @param ghost ,the ghost to be destroyed
     */
    public void destroy(Ghost ghost) {
        gamePane.getChildren().remove(ghost.getNode());

    }



    /*
      Getters and setter methods
     */

    public VBox getRoot() {
        return root;
    }

    public Label getLabelMessageBanner() {
        return labelMessageBanner;
    }

    public Pane getGamePane() {
        return gamePane;
    }

    public List<Ghost> getGhosts() {
        return ghosts;
    }
}
