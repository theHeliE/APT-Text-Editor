package com.springtest.apt_project_fe;

import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class DocumentController {
    @FXML private TextArea textArea;
    @FXML
    private AnchorPane rootPane; // You must fx:id the root AnchorPane!
    private double xOffset = 0;
    private double yOffset = 0;
    // Called by the loader after FXML is loaded
    public void initialize() {
        System.out.println("textArea = " + textArea);
        // nothing here unless you need startup logic

        rootPane.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        // Set mouse dragged event
        rootPane.setOnMouseDragged(event -> {
            rootPane.getScene().getWindow().setX(event.getScreenX() - xOffset);
            rootPane.getScene().getWindow().setY(event.getScreenY() - yOffset);
        });
    }

    // Populate the TextArea
    public void setFileContent(String content) {
        textArea.setText(content);
    }

    @FXML
    private void handleMinimize(ActionEvent event) {
        Stage stage = (Stage) ((JFXButton) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((JFXButton) event.getSource()).getScene().getWindow();
        stage.close();
    }

    // Optional (if you want a maximize toggle):
    @FXML
    private void handleMaximize(ActionEvent event) {
        Stage stage = (Stage) ((JFXButton) event.getSource()).getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

}
