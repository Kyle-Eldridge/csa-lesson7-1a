package com.codedotorg;

import java.util.ArrayList;

import com.codedotorg.modelmanager.CameraController;
import com.codedotorg.modelmanager.ModelManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GuessingGame {

    /** The main window of the app */
    private Stage window;

    /** The main scene of the app to display the game */
    private MainScene game;

    /** The GameOverScene to display the winner */
    private GameOverScene gameOver;

    /** The GameLogic to handle the game logic */
    private GameLogic logic;

    /** Manages the TensorFlow model used for image classification */
    private ModelManager model;

    /** Controls the camera capture and provides frames to the TensorFlow model for classification */
    private CameraController cameraController;

    /** The Timeline to manage how often a prediction is made */
    private Timeline timeline;
    private Timeline timeline2;

    private boolean started;
    private boolean paused = false;

    ArrayList<Integer> guesses = new ArrayList<>();

    /**
     * Constructor for the GuessingGame class.
     * Sets up the window using the primaryStage, initializes the model
     * and camera capture, sets up the game scenes and logic.
     *
     * @param primaryStage the primary stage for the application
     */
    public GuessingGame(Stage primaryStage) {
        // Set up the window using the primaryStage
        setUpWindow(primaryStage);

        // Set up the model and camera capture
        cameraController = new CameraController();
        model = new ModelManager();
        
        // Set up the game scenes and logic
        game = new MainScene();
        gameOver = new GameOverScene();
        logic = new GameLogic();

        started = false;
    }

    /**
     * Sets up the window with the given primaryStage, sets the title of
     * the window to "Guessing Game", and adds a shutdown hook to stop
     * the camera capture when the app is closed.
     *
     * @param primaryStage the primary stage of the application
     */
    public void setUpWindow(Stage primaryStage) {
        // Set window to point to the primaryStage
        window = primaryStage;

        // Set the title of the window
        window.setTitle("Guessing Game");

        // Shutdown hook to stop the camera capture when the app is closed
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cameraController.stopCapture();
        }));
    }
    
    /**
     * Starts a new guessing game by loading the main
     * screen and updating the game state.
     */
    public void playGame() {
        loadMainScreen();
        updateGame();
    }

    /**
     * Loads the main screen of the game, setting it to starting defaults
     * and displaying the window. Captures the camera view and sets the model
     * for the cameraController object. Retrieves the Loading object and
     * shows the loading animation while the camera is loading.
     */
    public void loadMainScreen() {
        // Set the game to starting defaults
        resetGame();

        // Display the window
        window.show();

        // Capture the camera view and set the model for the cameraController object
        cameraController.captureCamera(game.getCameraView(), model);

        // Retrieve the Loading object
        Loading cameraLoading = game.getLoadingAnimation();

        // Show the loading animation while the camera is loading
        cameraLoading.showLoadingAnimation(game.getCameraView());
    }

    /**
     * Updates the game by getting the predicted class and score from the CameraController,
     * showing the user's response and confidence score in the app, getting the result of the
     * computer's guess, and ending the game if the guess is correct. If the guess is incorrect,
     * the computer's guess is displayed in the app.
     */
    private void updateGame() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(10), event -> {
            // Get the predicted class and score from the CameraController
            String predictedClass = cameraController.getPredictedClass();
            double predictedScore = cameraController.getPredictedScore();
            
            if (predictedClass != null) {
                started = true;
                // Show the user's response and confidence score in the app
                game.showUserResponse(predictedClass, predictedScore);

                
                // End the game if the guess is correct
                if (logic.isGuessCorrect(predictedClass)) {
                    loadGameOver(logic.binarySearch(predictedClass));
                }
                else {
                    // Get the result of the computer's guess
                    int result = logic.binarySearch(predictedClass);
                    // Create a String with the computer's guess
                    String computerGuess = "Computer Guess: " + result;

                    // Update the computer guess label with the number the computer guessed
                    game.showComputerResponse(computerGuess);
                }
            }
        }));
        timeline2 = new Timeline(new KeyFrame(Duration.seconds(0.1), event -> {
            if(started){
                // Get the predicted class and score from the CameraController
                String predictedClass = cameraController.getPredictedClass();
                double predictedScore = cameraController.getPredictedScore();

                if (predictedClass != null) {
                    // Show the user's response and confidence score in the app
                    game.showUserResponse(predictedClass, predictedScore);
                }
            }
        }));
        
        // Specify that the animation should repeat indefinitely
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline2.setCycleCount(Timeline.INDEFINITE);

        // Start the animation
        timeline.play();
        timeline2.play();
    }

    /**
     * Loads the Game Over scene with the player's number and sets the
     * playAgainButton to reset the game when clicked. Stops the timeline.
     *
     * @param number The number that the computer guessed.
     */
    public void loadGameOver(int number) {
        // Retrieve the playAgainButton from the GameOverScene
        Button playAgainButton = gameOver.getPlayAgainButton();

        // Set the playAgainButton to reset the game when clicked
        playAgainButton.setOnAction(event -> {
            resetGame();
        });

        guesses.add(logic.getGuessCount());
        // Create the GameOverScene layout
        Scene gameOverScene = gameOver.createGameOverScene(number, guesses, cameraController);

        // Set the GameOverScene in the window
        window.setScene(gameOverScene);

        // Stop the timeline
        timeline.stop();
        timeline2.stop();
    }

    /**
     * Resets the game by resetting the game logic, creating a new main scene,
     * and setting the window to display the new scene. If a timeline is currently
     * running, it is resumed.
     */
    public void resetGame() {
        started = false;
        paused = false;

        // Reset the GameLogic
        logic.resetLogic();

        // Create the MainScene for the game
        Scene mainScene = game.createMainScene(cameraController);
        game.getPauseButton().setOnMouseClicked(event -> {
            pause();
        });

        // Set the MainScene in the window
        window.setScene(mainScene);
        
        // Play the timeline if it is not null
        if (timeline != null) {
            timeline.play();
        }
        if (timeline2 != null) {
            timeline2.play();
        }
    }

    public void pause(){
        paused = !paused;
        System.out.println(paused);
        if(paused){
            game.getPauseButton().setText("Resume");
            timeline.pause();
        }else{
            game.getPauseButton().setText("Pause");
            timeline.play();
        }
    }
}
