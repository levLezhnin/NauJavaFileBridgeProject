package ru.LevLezhnin.NauJava.controller.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalTemplateAttributes {

    @Value("${app.grafana.url:http://localhost:3000}")
    private String grafanaUrl;

    @ModelAttribute
    public void addGrafanaUrl(Model model) {
        model.addAttribute("grafanaUrl", grafanaUrl);
    }

}
