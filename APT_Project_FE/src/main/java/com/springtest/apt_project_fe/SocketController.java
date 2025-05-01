package com.springtest.apt_project_fe;

import com.springtest.apt_project_fe.model.User;
import javafx.application.Platform;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * JavaFX WebSocket client controller for real-time document collaboration.
 * Works with the server's WebSocketController.
 */
public class SocketController {
    private String serverUrl;
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private String documentId;
    private User user;
    private boolean isConnected = false;

    // Callback handlers for various message types
    private Consumer<Set<User>> userListUpdateHandler;
    private Consumer<Map<String, Object>> operationHandler;
    private Consumer<Map<String, Object>> cursorUpdateHandler;
    private Consumer<String> connectionErrorHandler;

    /**
     * Creates a new WebSocket client controller.
     *
     * @param serverUrl The URL of the WebSocket server (e.g., "ws://localhost:8080/ws")
     */
    public SocketController(String serverUrl) {
        this.serverUrl = serverUrl;
        this.setupStompClient();
    }

    /**
     * Sets up the STOMP client over WebSocket.
     */
    private void setupStompClient() {
        // Create WebSocket client with SockJS support
        WebSocketClient webSocketClient = new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))
        );

        stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }


    /**
     * Connects to the WebSocket server.
     *
     * @return A CompletableFuture that completes when connection is established
     */
    public CompletableFuture<Boolean> connect() {
        CompletableFuture<Boolean> connectionFuture = new CompletableFuture<>();

        if (stompClient == null) {
            setupStompClient();
        }

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                stompSession = session;
                isConnected = true;
                connectionFuture.complete(true);
            }

            @Override
            public void handleException(StompSession session, StompCommand command,
                                        StompHeaders headers, byte[] payload, Throwable exception) {
                isConnected = false;
                if (connectionErrorHandler != null) {
                    Platform.runLater(() -> connectionErrorHandler.accept(exception.getMessage()));
                }
                connectionFuture.completeExceptionally(exception);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                isConnected = false;
                if (connectionErrorHandler != null) {
                    Platform.runLater(() -> connectionErrorHandler.accept(exception.getMessage()));
                }
                connectionFuture.completeExceptionally(exception);
            }
        };

        stompClient.connect(serverUrl, sessionHandler);
        return connectionFuture;
    }

    /**
     * Joins a document using a document code.
     *
     * @param documentCode The editor or viewer code for the document
     * @return A CompletableFuture that completes with the join response
     */
    public CompletableFuture<Map<String, Object>> joinDocument(String documentCode) {
        if (!isConnected) {
            CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Not connected to server"));
            return future;
        }

        CompletableFuture<Map<String, Object>> joinFuture = new CompletableFuture<>();

        // Subscribe to receive the join response
        stompSession.subscribe("/user/queue/join", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                Map<String, Object> response = (Map<String, Object>) payload;

                if (response.containsKey("error")) {
                    joinFuture.completeExceptionally(
                            new RuntimeException((String) response.get("error")));
                    return;
                }

                documentId = (String) response.get("documentId");
                user = new User((String) response.get("userId"), (String) response.get("userColor"), (Boolean) response.get("isEditor"));
                System.out.println(user.getId() + " " +  user.isEditor() + " joined document " + documentId);
                // Subscribe to document updates after successful join
                subscribeToDocumentUpdates();

                joinFuture.complete(response);
            }
        });

        // Send join request with document code
        stompSession.send("/app/join", documentCode);

        return joinFuture;
    }

    /**
     * Subscribes to all relevant document update topics.
     */
    private void subscribeToDocumentUpdates() {
        // Subscribe to user list updates
        stompSession.subscribe("/topic/document/" + documentId + "/users", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                System.out.println("Getting payload type for users");
                return List.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                System.out.println("Received users payload: " + payload);

                if (userListUpdateHandler != null) {
                    try {
                        // Print the raw payload to see its structure
                        System.out.println("Raw payload class: " + payload.getClass().getName());
                        System.out.println("Raw payload: " + payload);

                        // The payload is a List of Maps rather than List of Users
                        List<Map<String, Object>> userMaps = (List<Map<String, Object>>) payload;
                        System.out.println("User maps size: " + userMaps.size());

                        // Convert Maps to User objects manually
                        Set<User> users = new HashSet<>();
                        for (Map<String, Object> userMap : userMaps) {
                            System.out.println("Processing user map: " + userMap);

                            String id = (String) userMap.get("id");
                            String color = (String) userMap.get("color");
                            Boolean editor = (Boolean) userMap.get("editor");

                            System.out.println("User data: id=" + id + ", color=" + color + ", editor=" + editor);

                            // Use the regular constructor
                            User user = new User(id, color, editor);

                            // Set cursor position if it exists
                            if (userMap.containsKey("cursorPosition")) {
                                Object cursorPos = userMap.get("cursorPosition");
                                if (cursorPos instanceof Number) {
                                    user.setCursorPosition(((Number) cursorPos).intValue());
                                }
                                System.out.println("Set cursor position: " + user.getCursorPosition());
                            }

                            users.add(user);
                            System.out.println("Added user: " + user.getId() + " " + user.isEditor() + " joined document " + documentId);
                        }

                        System.out.println("Final user set size: " + users.size());
                        Platform.runLater(() -> {
                            System.out.println("Running userListUpdateHandler");
                            userListUpdateHandler.accept(users);
                            System.out.println("userListUpdateHandler completed");
                        });
                    } catch (Exception e) {
                        System.err.println("Error processing user list: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("userListUpdateHandler is null");
                }
            }
        });

        // Subscribe to CRDT operations
        stompSession.subscribe("/topic/document/" + documentId + "/operation", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (operationHandler != null) {
                    Map<String, Object> operation = (Map<String, Object>) payload;
                    Platform.runLater(() -> operationHandler.accept(operation));
                }
            }
        });

        // Subscribe to cursor updates
        stompSession.subscribe("/topic/document/" + documentId + "/cursor", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (cursorUpdateHandler != null) {
                    Map<String, Object> cursorUpdate = (Map<String, Object>) payload;
                    Platform.runLater(() -> cursorUpdateHandler.accept(cursorUpdate));
                }
            }
        });
    }

    /**
     * Requests the full document CRDT data.
     *
     * @return A CompletableFuture that completes with the document data
     */
    public CompletableFuture<List<Map<String, Object>>> getDocumentData() {
        if (!isConnected || documentId == null) {
            CompletableFuture<List<Map<String, Object>>> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Not connected or no document joined"));
            return future;
        }

        CompletableFuture<List<Map<String, Object>>> dataFuture = new CompletableFuture<>();

        // Subscribe to receive the document data response
        stompSession.subscribe("/user/queue/get", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return List.class;  // Changed back to Map.class since the response is an object
            }

            @SuppressWarnings("unchecked")
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    List<Map<String, Object>> response = (List<Map<String, Object>>) payload;
                    dataFuture.complete(response);
                } catch (ClassCastException e) {
                    dataFuture.completeExceptionally(
                            new RuntimeException("Failed to parse response: " + e.getMessage(), e));
                }
            }
        });

        // Send request for document data
        stompSession.send("/app/document/" + documentId + "/get", null);

        return dataFuture;
    }


    /**
     * Requests the list of users in the document.
     *
     * @return A CompletableFuture that completes with the user list
     */
    public CompletableFuture<List<Map<String, Object>>> getDocumentUsers() {
        if (!isConnected || documentId == null) {
            CompletableFuture<List<Map<String, Object>>> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Not connected or no document joined"));
            return future;
        }

        CompletableFuture<List<Map<String, Object>>> usersFuture = new CompletableFuture<>();

        // Subscribe to receive the users list response
        stompSession.subscribe("/user/queue/users", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                Map<String, Object> response = (Map<String, Object>) payload;

                if (response.containsKey("error")) {
                    usersFuture.completeExceptionally(
                            new RuntimeException((String) response.get("error")));
                    return;
                }

                List<Map<String, Object>> users = (List<Map<String, Object>>) response.get("users");
                usersFuture.complete(users);
            }
        });

        // Send request for users list
        stompSession.send("/app/document/" + documentId + "/users", null);

        return usersFuture;
    }

    /**
     * Sends a CRDT operation (insert or delete) to the server.
     *
     * @param operationType The type of operation ("insert" or "delete")
     * @param data Operation-specific data
     */
    public void sendOperation(String operationType, Map<String, Object> data) {
        if (!isConnected || documentId == null || !user.isEditor()) {
            return; // Can't send operations if not connected, not in a document, or not an editor
        }

        Map<String, Object> operation = new HashMap<>(data);
        operation.put("type", operationType);
        operation.put("userId", user.getId());

        stompSession.send("/app/document/" + documentId + "/operation", operation);
    }

    /**
     * Sends a cursor position update to the server.
     *
     * @param position The cursor position (can be any object structure as needed)
     */
    public void sendCursorUpdate(Object position) {
        if (!isConnected || documentId == null) {
            return; // Can't send updates if not connected or not in a document
        }

        Map<String, Object> cursorUpdate = new HashMap<>();
        cursorUpdate.put("userId", user.getId());
        cursorUpdate.put("position", position);

        stompSession.send("/app/document/" + documentId + "/cursor", cursorUpdate);
    }

    /**
     * Leaves the current document.
     */
    public void leaveDocument() {
        if (!isConnected || documentId == null) {
            return; // Not connected or not in a document
        }

        Map<String, Object> leaveRequest = new HashMap<>();
        leaveRequest.put("documentId", documentId);
        leaveRequest.put("userId", user.getId());

        stompSession.send("/app/leave", leaveRequest);

        // Reset document state
//        documentId = null;
//        userId = null;
//        isEditor = false;
    }

    /**
     * Disconnects from the WebSocket server.
     */
    public void disconnect() {
        if (isConnected) {
            if (documentId != null) {
                leaveDocument();
            }

            stompSession.disconnect();
            stompSession = null;
            isConnected = false;
        }
    }

    // Setter methods for callback handlers

    /**
     * Sets a handler for user list updates.
     *
     * @param handler Consumer that receives the updated user list
     */
    public void setUserListUpdateHandler(Consumer<Set<User>> handler) {
        this.userListUpdateHandler = handler;
    }

    /**
     * Sets a handler for CRDT operations.
     *
     * @param handler Consumer that receives operation details
     */
    public void setOperationHandler(Consumer<Map<String, Object>> handler) {
        this.operationHandler = handler;
    }

    /**
     * Sets a handler for cursor updates.
     *
     * @param handler Consumer that receives cursor update details
     */
    public void setCursorUpdateHandler(Consumer<Map<String, Object>> handler) {
        this.cursorUpdateHandler = handler;
    }

    /**
     * Sets a handler for connection errors.
     *
     * @param handler Consumer that receives error messages
     */
    public void setConnectionErrorHandler(Consumer<String> handler) {
        this.connectionErrorHandler = handler;
    }

    // Getter methods for client state

    public boolean isConnected() {
        return isConnected;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getUserId() {
        return user.getId();
    }

    public boolean isEditor() {
        return user.isEditor();
    }
}