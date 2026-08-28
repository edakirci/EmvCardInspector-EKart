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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReaderDiagnosticCommandTest {
    @Test
    void reportsWhenNoReaderIsAvailable() {
        CommandResult result = runCommand(new FakeReaderService(List.of(), false), "1\n0\n");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("No PC/SC reader found"));
        assertTrue(result.output().contains("Main menu:"));
    }

    @Test
    void rejectsReaderSelectionOutsideAvailableRange() {
        CommandResult result = runCommand(
                new FakeReaderService(List.of("Contact", "Contactless"), false),
                "1\n7\n0\n");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("Invalid reader selection"));
    }

    @Test
    void waitsForCardOnSelectedReaderAndReportsTimeout() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Contact", "Contactless"),
                false);

        CommandResult result = runCommand(readerService, "2\n1\n0\n");

        assertEquals(0, result.exitCode());
        assertEquals("Contactless", readerService.waitedReaderName);
        assertTrue(result.output().contains("No card detected within 15 seconds"));
    }

    @Test
    void sendsSelectPpseAndPrintsSeparatedResponseFields() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Contact", "Contactless"),
                true,
                successfulPpseResponse());

        CommandResult result = runCommand(readerService, "2\n1\n0\n");

        assertEquals(0, result.exitCode());
        assertNotNull(readerService.session.transmittedCommand);
        assertEquals("00A404000E325041592E5359532E444446303100",
                HexUtils.toHex(readerService.session.transmittedCommand.bytes()));
        assertTrue(readerService.session.closed);
        assertTrue(result.output().contains("Interface  : Contactless"));
        assertTrue(result.output().contains("Command       : SELECT PPSE"));
        assertTrue(result.output().contains("Raw Response  : 6F"));
        assertTrue(result.output().contains("Response Data : 6F"));
        assertTrue(result.output().contains("SW1           : 90"));
        assertTrue(result.output().contains("SW2           : 00"));
        assertTrue(result.output().contains("Status Word   : 9000"));
        assertTrue(result.output().contains("Status        : Success"));
        assertTrue(result.output().contains("TLV Parsing   : SUCCESS"));
        assertTrue(result.output().contains("- 6F | FCI Template | constructed"));
        assertTrue(result.output().contains("- 4F | Application Identifier | primitive"));
        assertTrue(result.output().contains("Payment Applications: 2"));
        assertTrue(result.output().contains("AID                : A0000000031010"));
        assertTrue(result.output().contains("Application Label  : VISA"));
        assertTrue(result.output().contains("Priority Indicator : 81"));
        assertTrue(result.output().contains("AID                : A0000000041010"));
        assertTrue(result.output().contains("Application Label  : MASTERCARD"));
    }

    @Test
    void skipsTlvParsingWhenStatusWordIsNotSuccessful() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Contactless"),
                true,
                new ApduResponse(new byte[0], 0x6700));

        CommandResult result = runCommand(readerService, "2\n0\n0\n");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("TLV Parsing   : SKIPPED"));
        assertTrue(result.output().contains("status word 6700 is not successful"));
        assertTrue(result.output().contains("response data is empty"));
        assertFalse(result.output().contains("TLV Tree:"));
    }

    @Test
    void skipsTlvParsingWhenSuccessfulResponseDataIsEmpty() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Contactless"),
                true,
                new ApduResponse(new byte[0], 0x9000));

        CommandResult result = runCommand(readerService, "2\n0\n0\n");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("TLV Parsing   : SKIPPED"));
        assertTrue(result.output().contains("Reason        : response data is empty"));
        assertFalse(result.output().contains("TLV Tree:"));
    }

    @Test
    void reportsMalformedSuccessfulPpseData() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Contactless"),
                true,
                new ApduResponse(HexUtils.fromHex("6F035A0201"), 0x9000));

        CommandResult result = runCommand(readerService, "2\n0\n0\n");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("TLV Parsing   : FAILED"));
        assertTrue(result.output().contains("Parse Error"));
    }

    @Test
    void sendsSelectPseForContactAndReturnsToMainMenu() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Contact", "Contactless"),
                true,
                successfulPpseResponse());

        CommandResult result = runCommand(readerService, "1\n0\n0\n");

        assertEquals(0, result.exitCode());
        assertNotNull(readerService.session.transmittedCommand);
        assertEquals("00A404000E315041592E5359532E444446303100",
                HexUtils.toHex(readerService.session.transmittedCommand.bytes()));
        assertTrue(readerService.session.closed);
        assertTrue(result.output().contains("Interface  : Contact"));
        assertTrue(result.output().contains("Command       : SELECT PSE"));
        assertTrue(result.output().contains("Connection closed. Returning to main menu."));
        assertEquals(2, countOccurrences(result.output(), "Main menu:"));
    }

    @Test
    void invalidInterfaceSelectionRedisplaysMainMenu() {
        CommandResult result = runCommand(new FakeReaderService(List.of("Contact"), true), "9\n0\n");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("Invalid interface selection."));
        assertEquals(2, countOccurrences(result.output(), "Main menu:"));
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

    private static int countOccurrences(String text, String value) {
        return text.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }

    private static ApduResponse successfulPpseResponse() {
        return new ApduResponse(HexUtils.fromHex(
                "6F43"
                        + "840E325041592E5359532E4444463031"
                        + "A531BF0C2E"
                        + "61124F07A0000000031010500456495341870181"
                        + "61184F07A0000000041010500A4D415354455243415244870102"),
                0x9000);
    }

    private static final class FakeReaderService implements CardReaderService {
        private final List<String> readerNames;
        private final boolean cardPresent;
        private final FakeCardSession session;
        private String waitedReaderName;

        private FakeReaderService(List<String> readerNames, boolean cardPresent) {
            this(readerNames, cardPresent, new ApduResponse(HexUtils.fromHex("6F03840101"), 0x9000));
        }

        private FakeReaderService(
                List<String> readerNames,
                boolean cardPresent,
                ApduResponse response) {
            this.readerNames = readerNames;
            this.cardPresent = cardPresent;
            this.session = new FakeCardSession(response);
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
        private final ApduResponse response;
        private ApduCommand transmittedCommand;
        private boolean closed;

        private FakeCardSession(ApduResponse response) {
            this.response = response;
        }

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
            return response;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
