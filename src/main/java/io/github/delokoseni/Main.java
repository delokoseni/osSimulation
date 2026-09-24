package io.github.delokoseni;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/io/github/delokoseni/main-view.fxml")
        );

        Scene scene = new Scene(loader.load(), 1400, 850);

        scene.getStylesheets().add(
                getClass().getResource(
                        "/io/github/delokoseni/styles.css"
                ).toExternalForm()
        );

        stage.setTitle("Моделирование операционной системы");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}