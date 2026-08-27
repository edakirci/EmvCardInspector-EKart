package com.emvcardinspector.app;

import com.emvcardinspector.reader.CardConnection;
import com.emvcardinspector.reader.CardReaderService;
import org.junit.jupiter.api.Test;

import javax.smartcardio.CardException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReaderDiagnosticCommandTest {
    @Test
    void reportsWhenNoReaderIsAvailable() {
        CommandResult result = runCommand(new FakeReaderService(List.of(), false), "");

        assertEquals(1, result.exitCode());
        assertTrue(result.output().contains("No PC/SC reader found"));
    }

    @Test
    void rejectsReaderSelectionOutsideAvailableRange() {
        CommandResult result = runCommand(
                new FakeReaderService(List.of("Contact", "Contactless"), false),
                "7\n");

        assertEquals(1, result.exitCode());
        assertTrue(result.output().contains("Invalid reader selection"));
    }

    @Test
    void waitsForCardOnSelectedReaderAndReportsTimeout() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Contact", "Contactless"),
                false);

        CommandResult result = runCommand(readerService, "1\n");

        assertEquals(2, result.exitCode());
        assertEquals("Contactless", readerService.waitedReaderName);
        assertTrue(result.output().contains("No card detected within 15 seconds"));
    }

    private static CommandResult runCommand(FakeReaderService readerService, String inputText) {
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        try (Scanner input = new Scanner(inputText);
             PrintStream output = new PrintStream(outputBytes, true, StandardCharsets.UTF_8)) {
            ReaderDiagnosticCommand command = new ReaderDiagnosticCommand(
                    readerService,
                    input,
                    output,
                    Duration.ofSeconds(15));
            int exitCode = command.run();
            return new CommandResult(exitCode, outputBytes.toString(StandardCharsets.UTF_8));
        }
    }

    private record CommandResult(int exitCode, String output) {
    }

    private static final class FakeReaderService implements CardReaderService {
        private final List<String> readerNames;
        private final boolean cardPresent;
        private String waitedReaderName;

        private FakeReaderService(List<String> readerNames, boolean cardPresent) {
            this.readerNames = readerNames;
            this.cardPresent = cardPresent;
        }

        @Override
        public List<String> listReaderNames() {
            return readerNames;
        }

        @Override
        public boolean waitForCard(String readerName, Duration timeout) {
            waitedReaderName = readerName;
            return cardPresent;
        }

        @Override
        public CardConnection connect(String readerName) throws CardException {
            throw new AssertionError("connect must not be called when no card is present");
        }
    }
}
