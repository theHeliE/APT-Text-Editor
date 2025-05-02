package com.springtest.apt_project_fe;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListCell;
import com.jfoenix.controls.JFXListView;
import com.springtest.apt_project_fe.model.CRDT;
import com.springtest.apt_project_fe.model.CharacterNode;
import com.springtest.apt_project_fe.model.User;
import com.springtest.apt_project_fe.model.CrdtOperation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

import static com.springtest.apt_project_fe.model.CRDT.fromSerialized;

public class DocumentController {
    @FXML private TextArea textArea;
    @FXML private JFXListView<User> userList;
    @FXML private AnchorPane rootPane;

    private double xOffset = 0;
    private double yOffset = 0;

    private String documentCode;
    private User me;
    private Set<User> users = new HashSet<>();

    private final String URL = "ws://localhost:8080/ws";

    public void initialize() {
        rootPane.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        rootPane.setOnMouseDragged(event -> {
            rootPane.getScene().getWindow().setX(event.getScreenX() - xOffset);
            rootPane.getScene().getWindow().setY(event.getScreenY() - yOffset);
        });
    }

    public void setDocumentCode(String documentCode) {
        this.documentCode = documentCode;
        SocketController wsClient = new SocketController(URL);

        wsClient.setConnectionErrorHandler(error ->
                Platform.runLater(() -> System.err.println("Connection error: " + error))
        );

        wsClient.connect().thenAccept(connected -> {
            if (connected) {
                System.out.println("Connected to WebSocket server");

                handleOperation(wsClient);

                wsClient.setUserListUpdateHandler(updatedUsers -> {
                    Platform.runLater(() -> {
                        Set<User> updatedUserSet = new HashSet<>(updatedUsers);
                        if (!updatedUserSet.equals(users)) {
                            users.clear();
                            users.addAll(updatedUserSet);
                            populateListView();
                        }
                    });
                });

                wsClient.joinDocument(documentCode).thenAccept(response -> {
                    if (response.containsKey("error")) {
                        Platform.runLater(() -> System.out.println("Join error: " + response.get("error")));
                        return;
                    }

                    fetchDocument(wsClient);
                    me = wsClient.getUser();

                    wsClient.getDocumentUsers().thenAccept(userSet -> {
                        Platform.runLater(() -> {
                            users.clear();
                            users.addAll(userSet);
                            populateListView();
                        });
                    }).exceptionally(ex -> {
                        System.err.println("Error getting users: " + ex.getMessage());
                        return null;
                    });

                }).exceptionally(ex -> {
                    Platform.runLater(() -> {
                        System.err.println("Join error: " + ex.getMessage());
                        showErrorAndClose("Error", "Document Not Found",
                                "No document found with code '" + documentCode + "'", (Stage) rootPane.getScene().getWindow());
                    });
                    return null;
                });

            } else {
                Platform.runLater(() -> System.out.println("Failed to connect to server"));
            }
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                System.err.println("Connection failed: " + ex.getMessage());
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
        alert.setOnHidden(evt -> stageToClose.close());
        alert.showAndWait();
    }

    public void setFileContent(String content) {
        textArea.setText(content);
    }

    private void populateListView() {
        ObservableList<User> userListItems = FXCollections.observableArrayList(
                users != null ? users : Set.of()
        );

        if (userListItems.isEmpty()) {
            userListItems.add(new User("No users available", "#808080", false));
        }

        userList.setItems(userListItems);

        userList.setCellFactory(listView -> new JFXListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    String label = user.equals(me) ? "User: " + user.getId() + " (You)" : "User: " + user.getId();
                    setText(label);
                    try {
                        setTextFill(Color.web(user.getColor()));
                    } catch (IllegalArgumentException e) {
                        setTextFill(Color.BLACK);
                    }
                    setStyle("-fx-font-size: 13px; -fx-effect: dropshadow(one-pass-box, black, 2, 0.0, 0, 0);");
                }
            }
        });

        userList.setFocusTraversable(false);
    }

    private CRDT crdt;

    public void fetchDocument(SocketController wsClient) {
        wsClient.getDocumentData().thenAccept(documentData -> {
            try {
                crdt = CRDT.fromSerialized(documentData);
                Platform.runLater(() -> {
                    System.out.println("Document fetched: " + crdt.serialize());
                    setFileContent(crdt.getText());
                });
            } catch (Exception e) {
                Platform.runLater(() -> showErrorAndClose("Error", "Processing Error",
                        "Could not process document: " + e.getMessage(), (Stage) rootPane.getScene().getWindow()));
                e.printStackTrace();
            }
        }).exceptionally(ex -> {
            Platform.runLater(() -> showErrorAndClose("Error", "Retrieval Failed",
                    "Error retrieving document: " + ex.getMessage(), (Stage) rootPane.getScene().getWindow()));
            ex.printStackTrace();
            return null;
        });
    }

    public void handleOperation(SocketController wsClient) {
        // Handle incoming operations from other users
        wsClient.setOperationHandler(operation -> {
            Platform.runLater(() -> {
                // Skip operations initiated by the current user (they're already applied locally)
                if (me != null && operation.userId().equals(me.getId())) {
                    return;
                }

                // Apply the remote operation to the local CRDT
                try {
                    // Handle insert operation
                    if (operation.type().equals("insert")) {
                        crdt.insertCharacter(
                                operation.userId(),
                                operation.clock(),
                                operation.value(),
                                operation.parentId()
                        );
                    }
                    // Handle delete operation
                    else if (operation.type().equals("delete")) {
                        crdt.deleteCharacterById(operation.nodeId());
                    }

                    // Update the text area with the new CRDT state, maintaining cursor position
                    int caretPosition = textArea.getCaretPosition();
                    textArea.setText(crdt.getText());

                    // Try to maintain the cursor position if possible
                    try {
                        textArea.positionCaret(Math.min(caretPosition, textArea.getText().length()));
                    } catch (Exception e) {
                        System.err.println("Error repositioning caret: " + e.getMessage());
                    }
                } catch (Exception e) {
                    System.err.println("Error processing remote operation: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });

        // Set up local text change listener
        // Set up local text change listener
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (crdt == null || me == null) return; // Not initialized yet

            // Calculate the change
            if (oldValue.length() < newValue.length()) {
                // Character was inserted
                int changePos = findChangePosition(oldValue, newValue);
                char insertedChar = newValue.charAt(changePos);

                // Generate a timestamp for this operation
                String clock = generateClock();

                // Find the parent node ID based on insertion position
                String parentId;
                if (changePos == 0) {
                    // If inserting at the beginning, use the root node as parent
                    parentId = "root";
                } else {
                    // Otherwise, get the node at the position before the insertion
                    List<CharacterNode> nodes = crdt.getInOrderTraversal();
                    // We need the node that is BEFORE the insertion position
                    // Be careful with indices - may need adjustment based on your implementation
                    int parentIndex = Math.min(changePos - 1, nodes.size() - 1);
                    if (parentIndex >= 0 && parentIndex < nodes.size()) {
                        parentId = nodes.get(parentIndex).getId();
                    } else {
                        // Fallback to root if no valid parent found
                        parentId = "root";
                    }
                }

                // Apply to local CRDT first
                String newNodeId = crdt.insertCharacterAt(me.getId(), changePos, insertedChar, clock);

                if (newNodeId != null) {
                    // Send operation to server
                    CrdtOperation operation = new CrdtOperation(
                            "insert",
                            me.getId(),
                            clock,
                            newNodeId,  // Include the new node ID
                            parentId,   // Use the properly determined parent ID
                            insertedChar
                    );
                    wsClient.sendOperation(operation);
                }
            } else if (oldValue.length() > newValue.length()) {
                // Character was deleted - this part seems correct
                int deletePos = findChangePosition(newValue, oldValue);

                // Use deleteCharacter method which takes a position
                String deletedNodeId = crdt.deleteCharacter(deletePos);

                if (deletedNodeId != null) {
                    // Send operation to server
                    CrdtOperation operation = new CrdtOperation(
                            "delete",
                            me.getId(),
                            generateClock(),
                            deletedNodeId,
                            null,  // parentId not needed for delete
                            null   // value not needed for delete
                    );
                    wsClient.sendOperation(operation);
                }
            }
        });
    }

    // Helper method to find where a change occurred between two strings
    private int findChangePosition(String shorter, String longer) {
        int minLength = Math.min(shorter.length(), longer.length());

        // Find the position where the strings differ
        for (int i = 0; i < minLength; i++) {
            if (shorter.charAt(i) != longer.charAt(i)) {
                return i;
            }
        }

        // If we reached here, the difference is at the end
        return minLength;
    }

    // Helper method to generate a clock timestamp
    private String generateClock() {
        // This could be a simple timestamp or a logical clock
        return String.valueOf(System.currentTimeMillis());
    }

    @FXML
    private void handleMinimize(ActionEvent event) {
        ((Stage) ((JFXButton) event.getSource()).getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void handleClose(ActionEvent event) {
        ((Stage) ((JFXButton) event.getSource()).getScene().getWindow()).close();
    }

    @FXML
    private void handleMaximize(ActionEvent event) {
        Stage stage = (Stage) ((JFXButton) event.getSource()).getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }
}
