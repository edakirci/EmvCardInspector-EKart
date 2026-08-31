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
import java.util.ArrayList;
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
    void reportsWhenNoMatchingReaderIsAvailable() {
        CommandResult result = runCommand(
                new FakeReaderService(List.of("Identiv uTrust 5422 Smartcard Reader"), false),
                "2\n0\n");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("No contactless PC/SC reader found"));
        assertTrue(result.output().contains("Identiv uTrust 5422 Smartcard Reader"));
    }

    @Test
    void waitsForCardOnSelectedReaderAndReportsTimeout() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Identiv uTrust 5422 Smartcard Reader", "Identiv uTrust 5422CL"),
                false);

        CommandResult result = runCommand(readerService, "2\n0\n");

        assertEquals(0, result.exitCode());
        assertEquals("Identiv uTrust 5422CL", readerService.waitedReaderName);
        assertTrue(result.output().contains(
                "Reader selected automatically: Identiv uTrust 5422CL"));
        assertTrue(result.output().contains("No card detected within 15 seconds"));
    }

    @Test
    void sendsSelectPpseAndPrintsSeparatedResponseFields() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Contact", "Contactless"),
                true,
                successfulPpseResponse(),
                selectedVisaResponse(),
                successfulSingleRecordGpoResponse(),
                successfulApplicationRecordResponse(),
                selectedDebitMastercardResponse(),
                successfulSingleRecordGpoResponse(),
                successfulApplicationRecordResponse());

        CommandResult result = runCommand(readerService, "2\n0\n");

        assertEquals(0, result.exitCode());
        assertNotNull(readerService.session.transmittedCommand);
        assertEquals("00A404000E325041592E5359532E444446303100",
                HexUtils.toHex(readerService.session.transmittedCommands.get(0).bytes()));
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
        assertEquals(7, readerService.session.transmittedCommands.size());
        assertEquals("00A4040007A000000003101000",
                HexUtils.toHex(readerService.session.transmittedCommands.get(1).bytes()));
        assertEquals("80A80000048302079200",
                HexUtils.toHex(readerService.session.transmittedCommands.get(2).bytes()));
        assertEquals("00B2011400",
                HexUtils.toHex(readerService.session.transmittedCommands.get(3).bytes()));
        assertEquals("00A4040007A000000004101000",
                HexUtils.toHex(readerService.session.transmittedCommands.get(4).bytes()));
        assertEquals("80A80000048302079200",
                HexUtils.toHex(readerService.session.transmittedCommands.get(5).bytes()));
        assertEquals("00B2011400",
                HexUtils.toHex(readerService.session.transmittedCommands.get(6).bytes()));
        assertTrue(result.output().contains("Application Branch [0]"));
        assertTrue(result.output().contains("Scheme          : Visa"));
        assertTrue(result.output().contains("Application     : Debit Mastercard"));
        assertTrue(result.output().contains("PDOL            : 9F1A02"));
        assertTrue(result.output().contains("PDOL Data       : 0792"));
        assertTrue(result.output().contains("GET PROCESSING OPTIONS"));
        assertTrue(result.output().contains("Command         : READ RECORD 1 (SFI 2)"));
    }

    @Test
    void retriesSelectOnceWhenNewlyInsertedContactCardIsNotReady() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Contact", "Contactless"),
                true,
                new ApduResponse(new byte[0], 0x6D00),
                successfulPseResponse(),
                successfulPseRecordVisa(),
                selectedVisaResponse(),
                successfulGpoResponse(),
                successfulApplicationRecordResponse(),
                successfulApplicationRecordResponse(),
                successfulApplicationRecordResponse(),
                successfulApplicationRecordResponse(),
                successfulApplicationRecordResponse());

        CommandResult result = runCommand(readerService, "1\n0\n");

        assertEquals(0, result.exitCode());
        assertEquals(10, readerService.session.transmittedCommands.size());
        assertEquals("00A404000E315041592E5359532E444446303100",
                HexUtils.toHex(readerService.session.transmittedCommands.get(0).bytes()));
        assertEquals("00A404000E315041592E5359532E444446303100",
                HexUtils.toHex(readerService.session.transmittedCommands.get(1).bytes()));
        assertEquals("00B2010C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(2).bytes()));
        assertEquals("00A4040007A000000003101000",
                HexUtils.toHex(readerService.session.transmittedCommands.get(3).bytes()));
        assertEquals("80A8000002830000",
                HexUtils.toHex(readerService.session.transmittedCommands.get(4).bytes()));
        assertEquals("00B2021400",
                HexUtils.toHex(readerService.session.transmittedCommands.get(5).bytes()));
        assertEquals("00B2011C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(6).bytes()));
        assertEquals("00B2021C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(7).bytes()));
        assertEquals("00B2012C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(8).bytes()));
        assertEquals("00B2022C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(9).bytes()));
        assertTrue(result.output().contains("Card readiness: first SELECT returned 6D00"));
        assertTrue(result.output().contains("TLV Parsing   : SUCCESS"));
        assertTrue(result.output().contains("Payment Applications: 1"));
    }

    @Test
    void skipsTlvParsingWhenStatusWordIsNotSuccessful() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Contactless"),
                true,
                new ApduResponse(new byte[0], 0x6700));

        CommandResult result = runCommand(readerService, "2\n0\n");

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

        CommandResult result = runCommand(readerService, "2\n0\n");

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

        CommandResult result = runCommand(readerService, "2\n0\n");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("TLV Parsing   : FAILED"));
        assertTrue(result.output().contains("Parse Error"));
    }

    @Test
    void readsOnlyFirstPseDirectoryRecordForContactAndReturnsToMainMenu() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Contact", "Contactless"),
                true,
                successfulPseResponse(),
                successfulPseRecordVisa(),
                selectedVisaResponse(),
                successfulGpoResponse(),
                successfulApplicationRecordResponse(),
                successfulApplicationRecordResponse(),
                successfulApplicationRecordResponse(),
                successfulApplicationRecordResponse(),
                successfulApplicationRecordResponse());

        CommandResult result = runCommand(readerService, "1\n0\n");

        assertEquals(0, result.exitCode());
        assertEquals(9, readerService.session.transmittedCommands.size());
        assertEquals("00A404000E315041592E5359532E444446303100",
                HexUtils.toHex(readerService.session.transmittedCommands.get(0).bytes()));
        assertEquals("00B2010C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(1).bytes()));
        assertEquals("00A4040007A000000003101000",
                HexUtils.toHex(readerService.session.transmittedCommands.get(2).bytes()));
        assertEquals("80A8000002830000",
                HexUtils.toHex(readerService.session.transmittedCommands.get(3).bytes()));
        assertEquals("00B2021400",
                HexUtils.toHex(readerService.session.transmittedCommands.get(4).bytes()));
        assertEquals("00B2011C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(5).bytes()));
        assertEquals("00B2021C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(6).bytes()));
        assertEquals("00B2012C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(7).bytes()));
        assertEquals("00B2022C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(8).bytes()));
        assertTrue(readerService.session.closed);
        assertTrue(result.output().contains("Interface  : Contact"));
        assertTrue(result.output().contains("Command       : SELECT PSE"));
        assertTrue(result.output().contains("- 88 | Short File Identifier | primitive"));
        assertTrue(result.output().contains("PSE Directory SFI: 1"));
        assertTrue(result.output().contains("Command       : READ RECORD 1 (SFI 1)"));
        assertFalse(readerService.session.transmittedCommands.stream()
                .map(ApduCommand::bytes)
                .map(HexUtils::toHex)
                .anyMatch("00B2020C00"::equals));
        assertTrue(result.output().contains("Payment Applications: 1"));
        assertTrue(result.output().contains("AID                : A0000000031010"));
        assertTrue(result.output().contains("Application Branch [0]"));
        assertTrue(result.output().contains("Application     : Visa Debit"));
        assertTrue(result.output().contains("Command         : READ RECORD 2 (SFI 2)"));
        assertTrue(result.output().contains("APDU            : 00B2021400"));
        assertTrue(result.output().contains("APDU            : 00B2011C00"));
        assertTrue(result.output().contains("APDU            : 00B2021C00"));
        assertTrue(result.output().contains("APDU            : 00B2012C00"));
        assertTrue(result.output().contains("APDU            : 00B2022C00"));
        assertTrue(result.output().contains("Record Parsing  : SUCCESS"));
        assertTrue(result.output().contains("Connection closed. Returning to main menu."));
        assertEquals(2, countOccurrences(result.output(), "Main menu:"));
    }

    @Test
    void stopsContactFlowAfterFirstApplicationCompletesItsAflRecords() {
        FakeReaderService readerService = new FakeReaderService(
                List.of("Contact", "Contactless"),
                true,
                successfulPseResponse(),
                successfulPseRecordWithTwoApplications(),
                selectedVisaResponse(),
                successfulGpoResponse(),
                successfulApplicationRecordResponse(),
                successfulApplicationRecordResponse(),
                successfulApplicationRecordResponse(),
                successfulApplicationRecordResponse(),
                successfulApplicationRecordResponse());

        CommandResult result = runCommand(readerService, "1\n0\n");

        assertEquals(0, result.exitCode());
        assertEquals(9, readerService.session.transmittedCommands.size());
        assertEquals("80A8000002830000",
                HexUtils.toHex(readerService.session.transmittedCommands.get(3).bytes()));
        assertEquals("00B2021400",
                HexUtils.toHex(readerService.session.transmittedCommands.get(4).bytes()));
        assertEquals("00B2011C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(5).bytes()));
        assertEquals("00B2021C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(6).bytes()));
        assertEquals("00B2012C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(7).bytes()));
        assertEquals("00B2022C00",
                HexUtils.toHex(readerService.session.transmittedCommands.get(8).bytes()));
        assertFalse(readerService.session.transmittedCommands.stream()
                .map(ApduCommand::bytes)
                .map(HexUtils::toHex)
                .anyMatch("00A4040007A000000004101000"::equals));
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

    private static ApduResponse successfulPseResponse() {
        return new ApduResponse(HexUtils.fromHex(
                "6F15840E315041592E5359532E4444463031A503880101"), 0x9000);
    }

    private static ApduResponse successfulPseRecordVisa() {
        return new ApduResponse(HexUtils.fromHex(
                "701461124F07A0000000031010500456495341870181"), 0x9000);
    }

    private static ApduResponse successfulPseRecordWithTwoApplications() {
        return new ApduResponse(HexUtils.fromHex(
                "702A"
                        + "61124F07A0000000031010500456495341870181"
                        + "61144F07A000000004101050064D4153544552870102"),
                0x9000);
    }

    private static ApduResponse selectedVisaResponse() {
        return new ApduResponse(HexUtils.fromHex(
                "6F1D8407A0000000031010A512"
                        + "500A56697361204465626974"
                        + "9F38039F1A02"), 0x9000);
    }

    private static ApduResponse selectedDebitMastercardResponse() {
        return new ApduResponse(HexUtils.fromHex(
                "6F368407A0000000041010A52B"
                        + "50104465626974204D617374657263617264"
                        + "9F12104465626974204D617374657263617264"
                        + "9F38039F1A02"), 0x9000);
    }

    private static ApduResponse successfulGpoResponse() {
        return new ApduResponse(HexUtils.fromHex(
                "771282023800940C"
                        + "10020201"
                        + "18010200"
                        + "28010200"), 0x9000);
    }

    private static ApduResponse successfulSingleRecordGpoResponse() {
        return new ApduResponse(HexUtils.fromHex(
                "770A82022000940410010100"), 0x9000);
    }

    private static ApduResponse successfulApplicationRecordResponse() {
        return new ApduResponse(HexUtils.fromHex("700482023800"), 0x9000);
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
                ApduResponse... responses) {
            this.readerNames = readerNames;
            this.cardPresent = cardPresent;
            this.session = new FakeCardSession(responses);
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
        private final List<ApduResponse> responses;
        private final List<ApduCommand> transmittedCommands = new ArrayList<>();
        private ApduCommand transmittedCommand;
        private boolean closed;

        private FakeCardSession(ApduResponse... responses) {
            this.responses = List.of(responses);
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
            transmittedCommands.add(command);
            int responseIndex = transmittedCommands.size() - 1;
            if (responseIndex >= responses.size()) {
                throw new AssertionError("No fake response configured for APDU " + responseIndex);
            }
            return responses.get(responseIndex);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
