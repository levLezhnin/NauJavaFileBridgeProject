package ru.LevLezhnin.NauJava.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class MonitoringAuthController {

    @GetMapping("/visualizer-access")
    public ResponseEntity<Void> checkAccess() {
        return ResponseEntity.ok().build();
    }
}
