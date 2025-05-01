package com.server.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Document {
    private final String id;
    private final String name;
    private final CRDT crdt;
    private final String editorCode;
    private final String viewerCode;
    private Integer usersCount; // For userId generation
    private final Set<User> users;

    public Document(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.crdt = new CRDT();
        this.editorCode = UUID.randomUUID().toString().substring(0, 8);
        this.viewerCode = UUID.randomUUID().toString().substring(0, 8);
        this.usersCount = 0;
        this.users = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CRDT getCrdt() {
        return crdt;
    }

    public String getEditorCode() {
        return editorCode;
    }

    public String getViewerCode() {
        return viewerCode;
    }

    public Set<User> getUsers() {
        return users;
    }

    public boolean isEditor(String code) {
        return editorCode.equals(code);
    }

    public User createNewUser(boolean isEditor) {
        String color = generateRandomColor();
        User user = new User(String.valueOf(++usersCount), color, isEditor);
        users.add(user);
        return user;
    }

    public boolean removeUser(String userId) {
        return users.removeIf(user -> user.getId().equals(userId));
    }

    public void importContent(String userId, String content) {
        crdt.importContent(userId, content);
    }

    private String generateRandomColor() {
        int r = (int)(Math.random() * 127) + 128; // 128-255
        int g = (int)(Math.random() * 127) + 128; // 128-255
        int b = (int)(Math.random() * 127) + 128; // 128-255
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
