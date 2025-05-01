package com.server.model;

public class User {
    private final String id;
    private final String color;
    private final Boolean isEditor;
    private Integer cursorPosition;

    public User(String id, String color, Boolean isEditor) {
        this.id = id;
        this.color = color;
        this.isEditor = isEditor;
        this.cursorPosition = -1;
    }

    public String getId() {
        return id;
    }

    public String getColor() {
        return color;
    }

    public Boolean isEditor() {
        return isEditor;
    }

    public Integer getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(Integer cursorPosition) {
        this.cursorPosition = (cursorPosition == null) ? -1 : cursorPosition;
    }
}