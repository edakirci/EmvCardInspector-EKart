package com.emvcardinspector.app;

import com.emvcardinspector.apdu.ApduCommand;
import com.emvcardinspector.apdu.ApduResponse;
import com.emvcardinspector.apdu.StatusWord;
import com.emvcardinspector.emv.EmvApplication;
import com.emvcardinspector.emv.EmvCommands;
import com.emvcardinspector.emv.EmvDataException;
import com.emvcardinspector.emv.EmvTagDictionary;
import com.emvcardinspector.emv.PpseResponse;
import com.emvcardinspector.emv.PpseResponseParser;
import com.emvcardinspector.reader.CardReaderService;
import com.emvcardinspector.reader.CardSession;
import com.emvcardinspector.tlv.TlvNode;
import com.emvcardinspector.tlv.TlvParseException;
import com.emvcardinspector.util.HexUtils;

import javax.smartcardio.CardException;
import java.io.PrintStream;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/** Interactive PC/SC diagnostic and approved read-only card operations. */
public final class ReaderDiagnosticCommand {
    private final CardReaderService readerService;
    private final Scanner input;
    private final PrintStream output;
    private final Duration cardWaitTimeout;
    private final PpseResponseParser ppseResponseParser;
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

            output.println();
            output.println("Available PC/SC readers:");
            for (int index = 0; index < readers.size(); index++) {
                output.printf("[%d] %s%n", index, readers.get(index));
            }
            output.print("Select reader (or B to go back): ");
            if (!input.hasNextLine()) {
                return;
            }

            String readerSelection = input.nextLine().trim();
            if (readerSelection.equalsIgnoreCase("B")) {
                return;
            }
            Integer selection = parseSelection(readerSelection, readers.size());
            if (selection == null) {
                output.println("Invalid reader selection.");
                return;
            }

            String readerName = readers.get(selection);
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
            PpseResponse ppseResponse = ppseResponseParser.parse(response.data());
            output.println("TLV Parsing   : SUCCESS");
            printTlvTree(ppseResponse.tlvNodes());
            printApplications(ppseResponse.applications());
        } catch (TlvParseException | EmvDataException error) {
            output.println("TLV Parsing   : FAILED");
            output.println("Parse Error   : " + error.getMessage());
        }
    }

    private void printTlvTree(List<TlvNode> nodes) {
        output.println();
        output.println("TLV Tree:");
        for (TlvNode node : nodes) {
            printTlvNode(node, 0);
        }
    }

    private void printTlvNode(TlvNode node, int depth) {
        String indent = "  ".repeat(depth);
        String name = tagDictionary.find(node.tag().hex())
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
            printTlvNode(child, depth + 1);
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

    private static Integer parseSelection(String text, int readerCount) {
        try {
            int selection = Integer.parseInt(text.trim());
            return selection >= 0 && selection < readerCount ? selection : null;
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private enum PaymentInterface {
        CONTACT("Contact", "SELECT PSE") {
            @Override
            ApduCommand command() {
                return EmvCommands.selectPse();
            }
        },
        CONTACTLESS("Contactless", "SELECT PPSE") {
            @Override
            ApduCommand command() {
                return EmvCommands.selectPpse();
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

        abstract ApduCommand command();
    }
}
