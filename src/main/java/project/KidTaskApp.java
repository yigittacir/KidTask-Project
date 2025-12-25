package project;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import project.gui.LoginView;

public class KidTaskApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        // try to set the application icon (place logo.png into src/main/resources)
        try {
            Image ic = new Image(getClass().getResourceAsStream("/logo.png"));
            if (ic != null) primaryStage.getIcons().add(ic);
        } catch (Exception ex) {
            // ignore if icon not found
        }
        new LoginView().show(primaryStage);
    }
    public static void main(String[] args) {
        launch(args);
    }
}