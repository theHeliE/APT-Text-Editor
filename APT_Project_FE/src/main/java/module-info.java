module com.springtest.apt_project_fe {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.springtest.apt_project_fe to javafx.fxml;
    exports com.springtest.apt_project_fe;
}