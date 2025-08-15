package com.mylibrary;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class PrimaryController {

    @FXML
    private void switchToSecondary(ActionEvent event) {
        try {
            LibraryUI ui = new LibraryUI();
            Stage stage = new Stage();
            ui.start(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
