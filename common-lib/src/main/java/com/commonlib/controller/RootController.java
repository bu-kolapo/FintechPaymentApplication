package com.commonlib.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.result.view.RedirectView;

@RestController
public class RootController {

    @Value("${spring.application.name:unknown-service}")
    private String appName;

    @Value("${info.app.version:1.0.0-SNAPSHOT}")
    private String version;

    /**
     * Root ("/") endpoint - redirects to /actuator/info
     */
    @GetMapping("/")
    public RedirectView rootRedirect() {
        return new RedirectView("/actuator/info");
    }

    /**
     * Optional: Simple /status endpoint
     */
    @GetMapping("/status")
    public String status() {
        return appName + " is running (version: " + version + ")";
    }
}
