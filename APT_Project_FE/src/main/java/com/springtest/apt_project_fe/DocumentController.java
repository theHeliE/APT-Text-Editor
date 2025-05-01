package com.springtest.apt_project_fe;

import com.jfoenix.controls.JFXButton;
import com.springtest.apt_project_fe.model.CRDT;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.springtest.apt_project_fe.model.CRDT.fromSerialized;

public class DocumentController {
    @FXML private TextArea textArea;
    @FXML
    private AnchorPane rootPane; // You must fx:id the root AnchorPane!
    private double xOffset = 0;
    private double yOffset = 0;

    private String documentCode;

    private String URL = "ws://localhost:8080/ws";
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

    public void setDocumentCode(String documentCode) {
        this.documentCode = documentCode;


        // Show loading indicator if you have one
        // loadingIndicator.setVisible(true);

//        // Create controller with proper URL
        SocketController wsClient = new SocketController(URL);
//
//        // Set detailed error handlers
        wsClient.setConnectionErrorHandler(error -> {
            Platform.runLater(() -> {
                System.err.println("Connection error details: " + error);
            });
        });

        // First connect, then join document only after connection is successful
        wsClient.connect()
                .thenAccept(connected -> {
                    if (connected) {
                        System.out.println("Successfully connected to WebSocket server");

                        // Only attempt to join after connection is confirmed
                        wsClient.joinDocument(documentCode)
                                .thenAccept(response -> {
                                    if (response.containsKey("error")) {
                                        Platform.runLater(() ->
                                                System.out.println("Error joining document: " + response.get("error")));
                                        return;
                                    }
                                    fetchDocument(wsClient);

                                })
                                .exceptionally(ex -> {
                                    Platform.runLater(() -> {
                                        System.out.println("Error joining document: " + ex.getMessage());
                                        System.err.println("Join document error details: " + ex);
                                        //ex.printStackTrace();
                                        Stage currentStage = (Stage) rootPane.getScene().getWindow();
                                        showErrorAndClose("Error", "Document Not Found",
                                                "No document found with code '" + documentCode + "'", currentStage);
                                    });
                                    return null;
                                });
                    } else {
                        Platform.runLater(() ->
                                System.out.println("Failed to connect to server"));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        System.out.println("Connection failed: " + ex.getMessage());
                        System.err.println("Connection error details: " + ex);
                        ex.printStackTrace();
                    });
                    return null;
                });
    }

    private void showErrorAndClose(String title, String header, String content, Stage stageToClose) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // When OK button is clicked, close the stage
        alert.setOnHidden(evt -> stageToClose.close());

        alert.showAndWait();
    }




    // Populate the TextArea
    public void setFileContent(String content) {
        textArea.setText(content);
    }

    public void fetchDocument(SocketController wsClient) {
        // Get the document data asynchronously
        wsClient.getDocumentData()
                .thenAccept(documentData -> {
                    try {
                        // Create CRDT from the received data
                        CRDT crdt = CRDT.fromSerialized(documentData);

                        // Do whatever you need with the CRDT object
                        Platform.runLater(() -> {
                            // Update UI with the CRDT data
//                            String text = crdt.toString(); // Or however you extract text from CRDT
//                            textArea.setText(text);
                            System.out.println("Document fetched successfully " + crdt.serialize());

                            String text = crdt.getText();
                            setFileContent(text);

                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showErrorAndClose("Error", "Failed to process document",
                                    "Error processing document data: " + e.getMessage(),
                                    (Stage) rootPane.getScene().getWindow());
                        });
                        e.printStackTrace();
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showErrorAndClose("Error", "Failed to retrieve document",
                                "Error retrieving document data: " + ex.getMessage(),
                                (Stage) rootPane.getScene().getWindow());
                    });
                    ex.printStackTrace();
                    return null;
                });
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
