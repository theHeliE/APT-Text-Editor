package com.springtest.apt_project_fe;

import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

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
    public void switchToSceneFileViewer(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("FileViewer.fxml"));
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
}


