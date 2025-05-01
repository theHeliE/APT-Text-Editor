package com.springtest.apt_project_fe;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextArea;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class SceneController {
    private Stage stage;
    private Scene scene;
    private Parent root;



    public void switchToSceneNew(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("newdoc.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void switchToSceneMain(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("main.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    public void switchToSceneBrowser(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("browser.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    public void switchToSceneFileViewer(ActionEvent event, String documentCode) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("document.fxml"));

        // Load the FXML file to get the root node
        root = loader.load();

        // Now call getController() on the loader instance
        DocumentController documentController = loader.getController();
        documentController.setDocumentCode(documentCode);


        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
@FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((JFXButton) event.getSource()).getScene().getWindow();
        stage.close();
    }
    private double xOffset = 0;
    private double yOffset = 0;

    // Then inside your initialize() method (or after loading the scene), do this:
    @FXML
    private AnchorPane rootPane; // You must fx:id the root AnchorPane!


    public void initialize() {
        // Set mouse pressed event
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
    @FXML
    private void handleTextChange(javafx.scene.input.KeyEvent event) {
        JFXTextArea source = (JFXTextArea) event.getSource();
        JFXButton button = (JFXButton) source.getScene().lookup("#createButton");
        if (button != null) {
            button.setDisable(source.getText().isBlank());
        }
    }

    @FXML
    private JFXTextArea sessionCodeInput; // Add this field to reference the text area

    public void joinSession(ActionEvent actionEvent) throws IOException {
        String documentCode = sessionCodeInput.getText().trim();

        if (documentCode.isEmpty()) {
            showErrorMessage("Please enter a valid session code");
            return;
        }

        System.out.println("Joining session: " + documentCode);



        switchToSceneFileViewer(actionEvent, documentCode);

    }

    private void showErrorMessage(String message) {
        // Implement code to show an error message to the user
        // This could be a dialog, a label that becomes visible, etc.
        System.out.println("Error: " + message); // Temporary console output

        // Example with a simple alert:
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}


