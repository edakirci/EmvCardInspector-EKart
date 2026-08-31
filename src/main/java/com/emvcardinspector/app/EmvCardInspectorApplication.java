package com.emvcardinspector.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the local REST API used by the desktop application. */
@SpringBootApplication(scanBasePackages = "com.emvcardinspector")
public final class EmvCardInspectorApplication {
    private EmvCardInspectorApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(EmvCardInspectorApplication.class, args);
    }
}
