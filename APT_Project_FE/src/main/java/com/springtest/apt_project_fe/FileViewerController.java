package com.springtest.apt_project_fe;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class FileViewerController {
    @FXML private TextArea textArea;

    // Called by the loader after FXML is loaded
    public void initialize() {
        System.out.println("textArea = " + textArea);
        // nothing here unless you need startup logic
    }

    // Populate the TextArea
    public void setFileContent(String content) {
        textArea.setText(content);
    }
}
