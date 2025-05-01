package com.springtest.apt_project_fe.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    // JSON constructor for Jackson
    @JsonCreator
    public User(
            @JsonProperty("id") String id,
            @JsonProperty("color") String color,
            @JsonProperty("editor") Boolean isEditor,
            @JsonProperty("cursorPosition") Integer cursorPosition
    ) {
        this.id = id;
        this.color = color;
        this.isEditor = isEditor;
        this.cursorPosition = cursorPosition != null ? cursorPosition : -1;
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