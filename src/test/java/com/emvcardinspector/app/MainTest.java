package com.emvcardinspector.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MainTest {
    @Test
    void applicationHasMainEntryPoint() throws NoSuchMethodException {
        assertNotNull(Main.class.getDeclaredMethod("main", String[].class));
    }
}
