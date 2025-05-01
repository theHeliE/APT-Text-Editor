module com.springtest.apt_project_fe {
    requires javafx.fxml;
    requires com.jfoenix;
    requires javafx.controls;
    requires spring.messaging;
    requires spring.websocket;
    requires spring.web;
    requires java.desktop;
    requires com.fasterxml.jackson.annotation;


    opens com.springtest.apt_project_fe to javafx.fxml;
    exports com.springtest.apt_project_fe;
}