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
import java.util.concurrent.atomic.AtomicReference;

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
    SocketController wsClient = new SocketController(URL);

    Stack<CrdtOperation> undoStack = new Stack<>();
    Stack<CrdtOperation> redoStack = new Stack<>();

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

        wsClient.setConnectionErrorHandler(error -> {
            Platform.runLater(() -> {
                System.err.println("Connection error: " + error);

                // Add reconnection logic here
                if (error.contains("Connection closed")) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Connection Issue");
                    alert.setHeaderText("Connection Lost");
                    alert.setContentText("Connection to the document server was lost. " +
                            "This may happen if too many users are connected simultaneously. " +
                            "The application will attempt to reconnect.");
                    alert.show();

                    // Optional: Implement automatic reconnection attempts
                    // with exponential backoff
                }
            });
        });

        wsClient.connect().thenAccept(connected -> {
            if (connected) {
                System.out.println("Connected to WebSocket server");

                handleOperation(wsClient);
                handleCursorChange(wsClient);

                wsClient.setUserListUpdateHandler(updatedUsers -> {
                    System.out.println("User list update received with " + updatedUsers.size() + " users");
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

                    if (!me.isEditor()){
                        textArea.setEditable(false);
                    }

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
    private boolean ignoreTextChanges = false;


    public void setFileContent(String content) {
        ignoreTextChanges = true;

        // Update the text area with the content
        textArea.setText(content);

        // Position caret at the beginning or end as needed
        textArea.positionCaret(0);

        // Re-enable text change processing AFTER the update
        ignoreTextChanges = false;

        System.out.println("File content set without triggering operations: " + content);

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
                // Skip operations initiated by the current user
                if (me != null && operation.userId().equals(me.getId())) {
                    return;
                }

                try {
                    // Apply the remote operation to the local CRDT
                    if (operation.type().equals("insert")) {
                        crdt.insertCharacter(
                                operation.userId(),
                                operation.clock(),
                                operation.value(),
                                operation.parentId()
                        );
                    } else if (operation.type().equals("delete")) {
                        crdt.deleteCharacterById(operation.nodeId());
                    }
                    else if (operation.type().equals("undoDelete")) {
                        crdt.getNodeMap().get(operation.nodeId()).setDeleted(false);
                    }

                    // Set flag to ignore changes while we update the text area
                    ignoreTextChanges = true;
                    int caretPosition = textArea.getCaretPosition();
                    textArea.setText(crdt.getText());

                    try {
                        textArea.positionCaret(Math.min(caretPosition, textArea.getText().length()));
                    } catch (Exception e) {
                        System.err.println("Error repositioning caret: " + e.getMessage());
                    }

                    // Re-enable text change processing
                    ignoreTextChanges = false;
                } catch (Exception e) {
                    System.err.println("Error processing remote operation: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });

        // Set up local text change listener
        // Set up local text change listener
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (ignoreTextChanges || crdt == null || me == null) return;

            // Calculate the change
            if (oldValue.length() < newValue.length()) {
                // Character was inserted
                int caretPos = textArea.getCaretPosition();
                System.out.println("Caret position: " + caretPos);
                // The caret position is after the inserted character, so we need the position before

                int insertPos = caretPos;
                char insertedChar = newValue.charAt(insertPos);

                // Generate a timestamp for this operation
                String clock = generateClock();

                // Find the parent node ID based on insertion position
                String parentId;
                if (insertPos == 0) {
                    // If inserting at the beginning, use the root node as parent
                    parentId = "0:0";
                } else {
                    // Otherwise, get the node at the position before the insertion
                    List<CharacterNode> nodes = crdt.getInOrderTraversal();
                    // Get the node that is BEFORE the insertion position
                    int parentIndex = insertPos - 1;
                    System.out.println("Parent index: " + parentIndex);
                    if (parentIndex >= 0 && parentIndex < nodes.size()) {
                        parentId = nodes.get(parentIndex).getId();
                        System.out.println("parentId: " + parentId);
                        System.out.println("Parent value: " + nodes.get(parentIndex).getValue());
                    } else {
                        // Fallback to root if no valid parent found
                        parentId = "0:0";
                    }
                }

                // Apply to local CRDT first
                String newNodeId = crdt.insertCharacterAt(me.getId(), insertPos, insertedChar, clock);
                List<CharacterNode> nodes = crdt.getInOrderTraversal();
                if (newNodeId != null) {
                    // Send operation to server
                    CrdtOperation operation = new CrdtOperation(
                            "insert",
                            me.getId(),
                            clock,
                            newNodeId,
                            parentId,
                            insertedChar
                    );
                    undoStack.push(operation);
                    redoStack.clear();

//                    System.out.println("insertion index: " + insertPos);
//                    System.out.println("insert in nodes map: " + nodes.get(insertPos).getValue());
                    wsClient.sendOperation(operation);
                }
            } else if (oldValue.length() > newValue.length()) {
                // Character was deleted
                int caretPos = textArea.getCaretPosition();
                // For deletion, the caret position is already at the position where the character was deleted
                int deletePos = caretPos - 1;

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
                    undoStack.push(operation);
                    redoStack.clear();

                    wsClient.sendOperation(operation);
                }
            }
        });
    }

    private void handleCursorChange(SocketController wsClient) {
        // Create a timer for debouncing
        AtomicReference<Timer> cursorUpdateTimer = new AtomicReference<>(new Timer());

        textArea.caretPositionProperty().addListener((observable, oldValue, newValue) -> {
            // Cancel any pending update
            System.out.println("Cursor updated to " + newValue);
            cursorUpdateTimer.get().cancel();
            cursorUpdateTimer.get().purge();

            // Create a new timer
            cursorUpdateTimer.set(new Timer());

            // Schedule update after a short delay (e.g., 50ms)
            cursorUpdateTimer.get().schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        wsClient.sendCursorUpdate(newValue);
                    });
                }
            }, 50);
        });
    }

    private void handleLeaving(SocketController wsClient) {
        wsClient.disconnect();
    }

//    private void attemptReconnection(SocketController wsClient, int attemptCount) {
//        if (attemptCount > 5) {
//            Platform.runLater(() -> showErrorAndClose(
//                    "Connection Failed",
//                    "Unable to Reconnect",
//                    "Could not reconnect to the document server after multiple attempts.",
//                    (Stage) rootPane.getScene().getWindow())
//            );
//            return;
//        }
//
//        // Exponential backoff
//        int delay = (int) Math.pow(2, attemptCount) * 1000;
//
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                wsClient.connect().thenAccept(connected -> {
//                    if (connected) {
//                        Platform.runLater(() -> System.out.println("Reconnected to server"));
//                        wsClient.joinDocument(documentCode).thenAccept(response -> {
//                            if (!response.containsKey("error")) {
//                                fetchDocument(wsClient);
//                            }
//                        });
//                    } else {
//                        attemptReconnection(wsClient, attemptCount + 1);
//                    }
//                });
//            }
//        }, delay);
//    }

    // Helper method to generate a clock timestamp
    private String generateClock() {
        // This could be a simple timestamp or a logical clock
        return String.valueOf(System.currentTimeMillis());
    }

    @FXML
    private void handleUndo(ActionEvent event) {
        if (undoStack.isEmpty()) return;
        CrdtOperation operation = undoStack.pop();
        redoStack.push(operation);

        if(operation.type().equals("insert")) {
            crdt.deleteCharacterById(operation.nodeId());
            CrdtOperation undoOperation = new CrdtOperation("delete", operation.userId(), operation.clock(), operation.nodeId(), null, null);
            wsClient.sendOperation(undoOperation);
            setFileContent(crdt.getText());
        } else if (operation.type().equals("delete")) {
            crdt.getNodeMap().get(operation.nodeId()).setDeleted(false);
            CrdtOperation undoOperation = new CrdtOperation("undoDelete", operation.userId(), operation.clock(), operation.nodeId(), null, null);
            wsClient.sendOperation(undoOperation);
            setFileContent(crdt.getText());
        }
    }

    @FXML
    private void handleRedo(ActionEvent event) {
        if(redoStack.isEmpty()) return;
        CrdtOperation operation = redoStack.pop();
        undoStack.push(operation);

        if(operation.type().equals("insert")) {
            crdt.getNodeMap().get(operation.nodeId()).setDeleted(false);
            CrdtOperation redoOperation = new CrdtOperation("undoDelete", operation.userId(), operation.clock(), operation.nodeId(), null, null);
            wsClient.sendOperation(redoOperation);
            setFileContent(crdt.getText());
        } else if (operation.type().equals("delete")) {
            crdt.deleteCharacterById(operation.nodeId());
            CrdtOperation redoOperation = new CrdtOperation("delete", operation.userId(), operation.clock(), operation.nodeId(), null, null);
            wsClient.sendOperation(redoOperation);
            setFileContent(crdt.getText());
        }
    }

    @FXML
    private void handleMinimize(ActionEvent event) {
        ((Stage) ((JFXButton) event.getSource()).getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void handleClose(ActionEvent event) {
        handleLeaving(wsClient);
        wsClient.disconnect(); // Explicitly call disconnect

        // Close the window
        ((Stage) ((JFXButton) event.getSource()).getScene().getWindow()).close();

        // Add this to exit the application process completely
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void handleMaximize(ActionEvent event) {
        Stage stage = (Stage) ((JFXButton) event.getSource()).getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }
}
