package com.emvcardinspector.app;

import com.emvcardinspector.apdu.ApduCommand;
import com.emvcardinspector.apdu.ApduResponse;
import com.emvcardinspector.apdu.StatusWord;
import com.emvcardinspector.emv.EmvApplication;
import com.emvcardinspector.emv.EmvCommands;
import com.emvcardinspector.emv.EmvDataException;
import com.emvcardinspector.emv.EmvTagDictionary;
import com.emvcardinspector.emv.ApplicationSelectionResponse;
import com.emvcardinspector.emv.ApplicationSelectionResponseParser;
import com.emvcardinspector.emv.ApplicationFileLocatorEntry;
import com.emvcardinspector.emv.GetProcessingOptionsResponse;
import com.emvcardinspector.emv.GetProcessingOptionsResponseParser;
import com.emvcardinspector.emv.PaymentScheme;
import com.emvcardinspector.emv.PpseResponse;
import com.emvcardinspector.emv.PpseResponseParser;
import com.emvcardinspector.emv.PseResponse;
import com.emvcardinspector.emv.PseResponseParser;
import com.emvcardinspector.reader.CardReaderService;
import com.emvcardinspector.reader.CardSession;
import com.emvcardinspector.tlv.BerTlvParser;
import com.emvcardinspector.tlv.TlvNode;
import com.emvcardinspector.tlv.TlvParseException;
import com.emvcardinspector.util.HexUtils;

import javax.smartcardio.CardException;
import java.io.PrintStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

/** Interactive PC/SC diagnostic and approved read-only card operations. */
public final class ReaderDiagnosticCommand {
    private static final long CARD_READY_RETRY_DELAY_MILLIS = 250;

    private final CardReaderService readerService;
    private final Scanner input;
    private final PrintStream output;
    private final Duration cardWaitTimeout;
    private final PpseResponseParser ppseResponseParser;
    private final PseResponseParser pseResponseParser;
    private final ApplicationSelectionResponseParser applicationSelectionResponseParser;
    private final GetProcessingOptionsResponseParser getProcessingOptionsResponseParser;
    private final BerTlvParser applicationRecordParser;
    private final EmvTagDictionary tagDictionary;

    public ReaderDiagnosticCommand(
            CardReaderService readerService,
            Scanner input,
            PrintStream output,
            Duration cardWaitTimeout) {
        this.readerService = Objects.requireNonNull(readerService, "readerService");
        this.input = Objects.requireNonNull(input, "input");
        this.output = Objects.requireNonNull(output, "output");
        this.cardWaitTimeout = Objects.requireNonNull(cardWaitTimeout, "cardWaitTimeout");
        this.ppseResponseParser = new PpseResponseParser();
        this.pseResponseParser = new PseResponseParser();
        this.applicationSelectionResponseParser = new ApplicationSelectionResponseParser();
        this.getProcessingOptionsResponseParser = new GetProcessingOptionsResponseParser();
        this.applicationRecordParser = new BerTlvParser();
        this.tagDictionary = EmvTagDictionary.standard();
    }

    public int run() {
        output.println("EMV Card Inspector");
        while (true) {
            PaymentInterface paymentInterface = selectPaymentInterface();
            if (paymentInterface == null) {
                output.println("Goodbye.");
                return 0;
            }
            runSingleSession(paymentInterface);
        }
    }

    private PaymentInterface selectPaymentInterface() {
        while (true) {
            output.println();
            output.println("Main menu:");
            output.println("[1] Contact (SELECT PSE)");
            output.println("[2] Contactless (SELECT PPSE)");
            output.println("[0] Exit");
            output.print("Select interface: ");
            if (!input.hasNextLine()) {
                output.println();
                return null;
            }

            switch (input.nextLine().trim()) {
                case "0":
                    return null;
                case "1":
                    return PaymentInterface.CONTACT;
                case "2":
                    return PaymentInterface.CONTACTLESS;
                default:
                    output.println("Invalid interface selection.");
            }
        }
    }

    private void runSingleSession(PaymentInterface paymentInterface) {
        try {
            List<String> readers = readerService.listReaderNames();
            if (readers.isEmpty()) {
                output.println("No PC/SC reader found.");
                return;
            }

            Optional<String> matchingReader = paymentInterface.findReader(readers);
            if (matchingReader.isEmpty()) {
                output.printf("No %s PC/SC reader found.%n",
                        paymentInterface.displayName().toLowerCase(Locale.ROOT));
                output.println("Detected readers:");
                readers.forEach(reader -> output.println("- " + reader));
                return;
            }

            String readerName = matchingReader.get();
            output.printf("Reader selected automatically: %s%n", readerName);
            output.printf("Waiting for a %s card on %s...%n",
                    paymentInterface.displayName().toLowerCase(), readerName);
            if (!readerService.waitForCard(readerName, cardWaitTimeout)) {
                output.printf("No card detected within %d seconds.%n", cardWaitTimeout.toSeconds());
                return;
            }

            try (CardSession connection = readerService.connect(readerName)) {
                output.printf("Reader     : %s%n", connection.readerName());
                output.printf("Interface  : %s%n", paymentInterface.displayName());
                output.printf("ATR        : %s%n", HexUtils.toHex(connection.atr()));
                output.printf("Protocol   : %s%n", connection.protocol());
                output.println("Connection : SUCCESS");
                executeSelectDirectory(connection, paymentInterface);
            }
            output.println("Connection closed. Returning to main menu.");
        } catch (CardException error) {
            output.println("Connection : FAILED");
            output.println("Error      : " + error.getMessage());
            if (error.getCause() != null) {
                output.println("Cause      : " + error.getCause().getMessage());
            }
            output.println("Returning to main menu.");
        }
    }

    private void executeSelectDirectory(
            CardSession connection,
            PaymentInterface paymentInterface) throws CardException {
        ApduCommand command = paymentInterface.command();
        long startedAt = System.nanoTime();
        ApduResponse response = connection.transmit(command);
        if (shouldRetrySelect(response)) {
            output.printf("Card readiness: first SELECT returned %04X with empty data; "
                            + "retrying once after %d ms.%n",
                    response.statusWord(),
                    CARD_READY_RETRY_DELAY_MILLIS);
            waitBeforeSelectRetry();
            startedAt = System.nanoTime();
            response = connection.transmit(command);
        }
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;

        output.println();
        output.printf("Command       : %s%n", paymentInterface.commandName());
        output.printf("APDU          : %s%n", HexUtils.toHex(command.bytes()));
        output.printf("Raw Response  : %s%n", HexUtils.toHex(response.bytes()));
        output.printf("Response Data : %s%n",
                response.data().length == 0 ? "<empty>" : HexUtils.toHex(response.data()));
        output.printf("SW1           : %02X%n", response.sw1());
        output.printf("SW2           : %02X%n", response.sw2());
        output.printf("Status Word   : %04X%n", response.statusWord());
        output.printf("Status        : %s%n", StatusWord.describe(response.statusWord()));
        output.printf("Duration      : %d ms%n", durationMillis);

        if (!response.isSuccess()) {
            output.println("TLV Parsing   : SKIPPED");
            output.printf("Reason        : status word %04X is not successful%n", response.statusWord());
            if (response.data().length == 0) {
                output.println("                response data is empty");
            }
            return;
        }
        if (response.data().length == 0) {
            output.println("TLV Parsing   : SKIPPED");
            output.println("Reason        : response data is empty");
            return;
        }

        try {
            if (paymentInterface == PaymentInterface.CONTACT) {
                PseResponse pseResponse = pseResponseParser.parse(response.data());
                output.println("TLV Parsing   : SUCCESS");
                printTlvTree(pseResponse.tlvNodes());
                output.printf("PSE Directory SFI: %d%n", pseResponse.directorySfi());
                readFirstPseDirectoryRecord(connection, pseResponse.directorySfi());
            } else {
                PpseResponse ppseResponse = ppseResponseParser.parse(response.data());
                output.println("TLV Parsing   : SUCCESS");
                printTlvTree(ppseResponse.tlvNodes());
                printApplications(ppseResponse.applications());
                selectApplications(connection, ppseResponse.applications(), paymentInterface);
            }
        } catch (TlvParseException | EmvDataException error) {
            output.println("TLV Parsing   : FAILED");
            output.println("Parse Error   : " + error.getMessage());
        }
    }

    private boolean shouldRetrySelect(ApduResponse response) {
        return response.statusWord() == StatusWord.INSTRUCTION_NOT_SUPPORTED.code()
                && response.data().length == 0;
    }

    private void waitBeforeSelectRetry() throws CardException {
        try {
            Thread.sleep(CARD_READY_RETRY_DELAY_MILLIS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new CardException("Interrupted while waiting for the card to become ready", error);
        }
    }

    private void readFirstPseDirectoryRecord(CardSession connection, int sfi) throws CardException {
        List<EmvApplication> applications = new ArrayList<>();
        int recordNumber = 1;
        ApduCommand command = EmvCommands.readRecord(recordNumber, sfi);
        long startedAt = System.nanoTime();
        ApduResponse response = connection.transmit(command);
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;

        output.println();
        output.printf("Command       : READ RECORD %d (SFI %d)%n", recordNumber, sfi);
        output.printf("APDU          : %s%n", HexUtils.toHex(command.bytes()));
        output.printf("Raw Response  : %s%n", HexUtils.toHex(response.bytes()));
        output.printf("Status Word   : %04X%n", response.statusWord());
        output.printf("Status        : %s%n", StatusWord.describe(response.statusWord()));
        output.printf("Duration      : %d ms%n", durationMillis);

        if (!response.isSuccess()) {
            output.println("Record Parsing: SKIPPED");
            output.printf("Reason        : status word %04X is not successful%n", response.statusWord());
            printApplications(applications);
            return;
        }
        if (response.data().length == 0) {
            output.println("Record Parsing: SKIPPED");
            output.println("Reason        : response data is empty");
            printApplications(applications);
            return;
        }

        try {
            PpseResponse record = ppseResponseParser.parse(response.data());
            output.println("Record Parsing: SUCCESS");
            printTlvTree(record.tlvNodes());
            applications.addAll(record.applications());
        } catch (TlvParseException | EmvDataException error) {
            output.println("Record Parsing: FAILED");
            output.println("Parse Error   : " + error.getMessage());
        }
        printApplications(applications);
        selectApplications(connection, applications, PaymentInterface.CONTACT);
    }

    private void selectApplications(
            CardSession connection,
            List<EmvApplication> applications,
            PaymentInterface paymentInterface) throws CardException {
        for (int index = 0; index < applications.size(); index++) {
            EmvApplication application = applications.get(index);
            PaymentScheme scheme = PaymentScheme.fromAid(application.aid());
            ApduCommand command = EmvCommands.selectApplication(application.aid());
            long startedAt = System.nanoTime();
            ApduResponse response = connection.transmit(command);
            long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;

            output.println();
            output.printf("Application Branch [%d]%n", index);
            output.printf("  Scheme          : %s%n", scheme.displayName());
            output.printf("  AID             : %s%n", application.aidHex());
            output.printf("  Directory Label : %s%n", application.label().orElse("<not provided>"));
            output.printf("  Command         : SELECT AID%n");
            output.printf("  APDU            : %s%n", HexUtils.toHex(command.bytes()));
            output.printf("  Raw Response    : %s%n", HexUtils.toHex(response.bytes()));
            output.printf("  Status Word     : %04X%n", response.statusWord());
            output.printf("  Status          : %s%n", StatusWord.describe(response.statusWord()));
            output.printf("  Duration        : %d ms%n", durationMillis);

            if (!response.isSuccess()) {
                output.println("  FCI Parsing     : SKIPPED");
                output.printf("  Reason          : status word %04X is not successful%n", response.statusWord());
                continue;
            }
            if (response.data().length == 0) {
                output.println("  FCI Parsing     : SKIPPED");
                output.println("  Reason          : response data is empty");
                continue;
            }

            try {
                ApplicationSelectionResponse selected = applicationSelectionResponseParser.parse(response.data());
                String applicationName = selected.preferredName()
                        .or(() -> selected.label())
                        .or(() -> application.label())
                        .orElse("<not provided>");
                output.println("  FCI Parsing     : SUCCESS");
                output.printf("  Application     : %s%n", applicationName);
                output.printf("  Preferred Name  : %s%n", selected.preferredName().orElse("<not provided>"));
                output.printf("  PDOL            : %s%n", selected.pdolHex().orElse("<not provided>"));
                printTlvTree(selected.tlvNodes(), application.aid());
                if (paymentInterface == PaymentInterface.CONTACT) {
                    executeGetProcessingOptions(connection, application.aid());
                    return;
                }
            } catch (TlvParseException | EmvDataException error) {
                output.println("  FCI Parsing     : FAILED");
                output.println("  Parse Error     : " + error.getMessage());
            }
        }
    }

    private void executeGetProcessingOptions(CardSession connection, byte[] applicationAid) throws CardException {
        ApduCommand command = EmvCommands.getProcessingOptions();
        long startedAt = System.nanoTime();
        ApduResponse response = connection.transmit(command);
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;

        output.println();
        output.println("  Command         : GET PROCESSING OPTIONS");
        output.printf("  APDU            : %s%n", HexUtils.toHex(command.bytes()));
        output.printf("  Raw Response    : %s%n", HexUtils.toHex(response.bytes()));
        output.printf("  Status Word     : %04X%n", response.statusWord());
        output.printf("  Status          : %s%n", StatusWord.describe(response.statusWord()));
        output.printf("  Duration        : %d ms%n", durationMillis);

        if (!response.isSuccess()) {
            output.println("  GPO Parsing     : SKIPPED");
            output.printf("  Reason          : status word %04X is not successful%n", response.statusWord());
            return;
        }
        if (response.data().length == 0) {
            output.println("  GPO Parsing     : SKIPPED");
            output.println("  Reason          : response data is empty");
            return;
        }

        try {
            GetProcessingOptionsResponse gpo = getProcessingOptionsResponseParser.parse(response.data());
            output.println("  GPO Parsing     : SUCCESS");
            output.printf("  AIP (82)        : %s%n", HexUtils.toHex(gpo.aip()));
            output.printf("  AFL (94)        : %s%n", HexUtils.toHex(gpo.afl()));
            printTlvTree(gpo.tlvNodes(), applicationAid);
            readRecordsListedInAfl(connection, gpo.aflEntries(), applicationAid);
        } catch (TlvParseException | EmvDataException error) {
            output.println("  GPO Parsing     : FAILED");
            output.println("  Parse Error     : " + error.getMessage());
        }
    }

    private void readRecordsListedInAfl(
            CardSession connection,
            List<ApplicationFileLocatorEntry> aflEntries,
            byte[] applicationAid) throws CardException {
        for (ApplicationFileLocatorEntry entry : aflEntries) {
            output.printf("  AFL Entry       : SFI %d, records %d-%d, ODA records %d%n",
                    entry.sfi(),
                    entry.firstRecord(),
                    entry.lastRecord(),
                    entry.offlineAuthenticationRecordCount());

            // The fourth AFL byte only marks how many records participate in
            // offline data authentication; it does not remove them from READ RECORD.
            for (int recordNumber = entry.firstRecord();
                 recordNumber <= entry.lastRecord();
                 recordNumber++) {
                readApplicationRecord(connection, recordNumber, entry.sfi(), applicationAid);
            }
        }
    }

    private void readApplicationRecord(
            CardSession connection,
            int recordNumber,
            int sfi,
            byte[] applicationAid) throws CardException {
        ApduCommand command = EmvCommands.readRecord(recordNumber, sfi);
        long startedAt = System.nanoTime();
        ApduResponse response = connection.transmit(command);
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;

        output.println();
        output.printf("  Command         : READ RECORD %d (SFI %d)%n", recordNumber, sfi);
        output.printf("  APDU            : %s%n", HexUtils.toHex(command.bytes()));
        output.printf("  Raw Response    : %s%n", HexUtils.toHex(response.bytes()));
        output.printf("  Status Word     : %04X%n", response.statusWord());
        output.printf("  Status          : %s%n", StatusWord.describe(response.statusWord()));
        output.printf("  Duration        : %d ms%n", durationMillis);

        if (!response.isSuccess()) {
            output.println("  Record Parsing  : SKIPPED");
            output.printf("  Reason          : status word %04X is not successful%n", response.statusWord());
            return;
        }
        if (response.data().length == 0) {
            output.println("  Record Parsing  : SKIPPED");
            output.println("  Reason          : response data is empty");
            return;
        }

        try {
            List<TlvNode> nodes = applicationRecordParser.parse(response.data());
            output.println("  Record Parsing  : SUCCESS");
            printTlvTree(nodes, applicationAid);
        } catch (TlvParseException error) {
            output.println("  Record Parsing  : FAILED");
            output.println("  Parse Error     : " + error.getMessage());
        }
    }

    private void printTlvTree(List<TlvNode> nodes) {
        printTlvTree(nodes, null);
    }

    private void printTlvTree(List<TlvNode> nodes, byte[] applicationAid) {
        output.println();
        output.println("TLV Tree:");
        for (TlvNode node : nodes) {
            printTlvNode(node, 0, applicationAid);
        }
    }

    private void printTlvNode(TlvNode node, int depth, byte[] applicationAid) {
        String indent = "  ".repeat(depth);
        var tagDefinition = applicationAid == null
                ? tagDictionary.find(node.tag().hex())
                : tagDictionary.find(applicationAid, node.tag().hex());
        String name = tagDefinition
                .map(tag -> tag.name())
                .orElse("Unknown EMV tag");
        String type = node.tag().constructed() ? "constructed" : "primitive";
        output.printf("%s- %s | %s | %s | length=%d | offset=%d%n",
                indent,
                node.tag().hex(),
                name,
                type,
                node.length(),
                node.offset());
        if (!node.tag().constructed()) {
            output.printf("%s  Value: %s%n", indent, HexUtils.toHex(node.value()));
        }
        for (TlvNode child : node.children()) {
            printTlvNode(child, depth + 1, applicationAid);
        }
    }

    private void printApplications(List<EmvApplication> applications) {
        output.println();
        output.printf("Payment Applications: %d%n", applications.size());
        for (int index = 0; index < applications.size(); index++) {
            EmvApplication application = applications.get(index);
            output.printf("[%d] AID                : %s%n", index, application.aidHex());
            output.printf("    Application Label  : %s%n",
                    application.label().orElse("<not provided>"));
            output.printf("    Priority Indicator : %s%n",
                    application.priorityIndicator().isPresent()
                            ? "%02X".formatted(application.priorityIndicator().getAsInt())
                            : "<not provided>");
        }
    }

    private enum PaymentInterface {
        CONTACT("Contact", "SELECT PSE") {
            @Override
            ApduCommand command() {
                return EmvCommands.selectPse();
            }

            @Override
            boolean acceptsReader(String readerName) {
                return !isContactlessReader(readerName);
            }
        },
        CONTACTLESS("Contactless", "SELECT PPSE") {
            @Override
            ApduCommand command() {
                return EmvCommands.selectPpse();
            }

            @Override
            boolean acceptsReader(String readerName) {
                return isContactlessReader(readerName);
            }
        };

        private final String displayName;
        private final String commandName;

        PaymentInterface(String displayName, String commandName) {
            this.displayName = displayName;
            this.commandName = commandName;
        }

        String displayName() {
            return displayName;
        }

        String commandName() {
            return commandName;
        }

        Optional<String> findReader(List<String> readerNames) {
            return readerNames.stream().filter(this::acceptsReader).findFirst();
        }

        abstract boolean acceptsReader(String readerName);

        abstract ApduCommand command();

        private static boolean isContactlessReader(String readerName) {
            String normalizedName = readerName.toLowerCase(Locale.ROOT);
            return normalizedName.contains("contactless")
                    || normalizedName.contains("5422cl")
                    || normalizedName.contains("picc")
                    || normalizedName.contains("nfc");
        }
    }
}
