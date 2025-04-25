package com.springtest.apt_project_fe;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Browser {

    @FXML
    private Button browseButton;

    @FXML
    private void openFileExplorer(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select a Text File");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.md")
        );

        Window window = browseButton.getScene().getWindow();
        File selected = chooser.showOpenDialog(window);
        if (selected == null) return;

        try {
            // read file
            String content = Files.readString(selected.toPath(), StandardCharsets.UTF_8);

            // load viewer with absolute path
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/springtest/apt_project_fe/FileViewer.fxml")
            );
            Parent root = loader.load();

            // inject content
            FileViewerController viewer = loader.getController();
            viewer.setFileContent(content);

            // show in new window
            Stage stage = new Stage();
            stage.setTitle("Viewing: " + selected.getName());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            // optionally alert the user here
        }
    }


}
