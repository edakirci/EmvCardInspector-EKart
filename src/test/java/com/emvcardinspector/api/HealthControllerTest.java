package com.emvcardinspector.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {
    @Test
    void reportsThatBackendIsReady() {
        HealthController.HealthResponse response = new HealthController().health();

        assertEquals("ok", response.status());
        assertEquals("EMV Card Inspector backend is ready", response.message());
    }
}
