package com.mylibrary;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        new LibraryUI().start(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
