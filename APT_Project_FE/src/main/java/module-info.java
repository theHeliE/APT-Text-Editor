module com.springtest.apt_project_fe {
    requires javafx.fxml;
    requires com.jfoenix;
    requires javafx.controls;


    opens com.springtest.apt_project_fe to javafx.fxml;
    exports com.springtest.apt_project_fe;
}