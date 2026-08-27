package com.emvcardinspector.app;

import com.emvcardinspector.apdu.ApduCommand;
import com.emvcardinspector.apdu.ApduResponse;
import com.emvcardinspector.reader.CardReaderService;
import com.emvcardinspector.reader.CardSession;
import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import javax.smartcardio.CardException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void sendsSelectPpseAndPrintsSeparatedResponseFields() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Contact", "Contactless"),
                true);

        CommandResult result = runCommand(readerService, "1\n1\n");

        assertEquals(0, result.exitCode());
        assertNotNull(readerService.session.transmittedCommand);
        assertEquals("00A404000E325041592E5359532E444446303100",
                HexUtils.toHex(readerService.session.transmittedCommand.bytes()));
        assertTrue(readerService.session.closed);
        assertTrue(result.output().contains("Command       : SELECT PPSE"));
        assertTrue(result.output().contains("Raw Response  : 6F038401019000"));
        assertTrue(result.output().contains("Response Data : 6F03840101"));
        assertTrue(result.output().contains("SW1           : 90"));
        assertTrue(result.output().contains("SW2           : 00"));
        assertTrue(result.output().contains("Status Word   : 9000"));
        assertTrue(result.output().contains("Status        : Success"));
    }

    @Test
    void diagnosticOnlyOperationDoesNotSendAnApdu() {
        FakeReaderService readerService = new FakeReaderService(List.of("Contactless"), true);

        CommandResult result = runCommand(readerService, "0\n0\n");

        assertEquals(0, result.exitCode());
        assertEquals(null, readerService.session.transmittedCommand);
        assertTrue(readerService.session.closed);
        assertTrue(result.output().contains("No APDU command was sent"));
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
        private final FakeCardSession session = new FakeCardSession();
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
        public CardSession connect(String readerName) throws CardException {
            if (!cardPresent) {
                throw new AssertionError("connect must not be called when no card is present");
            }
            return session;
        }
    }

    private static final class FakeCardSession implements CardSession {
        private ApduCommand transmittedCommand;
        private boolean closed;

        @Override
        public String readerName() {
            return "Contactless";
        }

        @Override
        public byte[] atr() {
            return HexUtils.fromHex("3B80800101");
        }

        @Override
        public String protocol() {
            return "T=1";
        }

        @Override
        public ApduResponse transmit(ApduCommand command) {
            transmittedCommand = command;
            return new ApduResponse(HexUtils.fromHex("6F03840101"), 0x9000);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
