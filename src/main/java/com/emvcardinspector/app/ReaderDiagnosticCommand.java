package com.emvcardinspector.app;

import com.emvcardinspector.apdu.ApduCommand;
import com.emvcardinspector.apdu.ApduResponse;
import com.emvcardinspector.apdu.StatusWord;
import com.emvcardinspector.emv.EmvCommands;
import com.emvcardinspector.reader.CardReaderService;
import com.emvcardinspector.reader.CardSession;
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

    public ReaderDiagnosticCommand(
            CardReaderService readerService,
            Scanner input,
            PrintStream output,
            Duration cardWaitTimeout) {
        this.readerService = Objects.requireNonNull(readerService, "readerService");
        this.input = Objects.requireNonNull(input, "input");
        this.output = Objects.requireNonNull(output, "output");
        this.cardWaitTimeout = Objects.requireNonNull(cardWaitTimeout, "cardWaitTimeout");
    }

    public int run() {
        output.println("EMV Card Inspector");
        output.println();

        try {
            List<String> readers = readerService.listReaderNames();
            if (readers.isEmpty()) {
                output.println("No PC/SC reader found.");
                return 1;
            }

            output.println("Available PC/SC readers:");
            for (int index = 0; index < readers.size(); index++) {
                output.printf("[%d] %s%n", index, readers.get(index));
            }

            output.print("Select reader: ");
            if (!input.hasNextLine()) {
                output.println();
                output.println("Reader selection was not provided.");
                return 1;
            }

            Integer selection = parseSelection(input.nextLine(), readers.size());
            if (selection == null) {
                output.println("Invalid reader selection.");
                return 1;
            }

            String readerName = readers.get(selection);
            output.printf("Waiting for a card on %s...%n", readerName);
            if (!readerService.waitForCard(readerName, cardWaitTimeout)) {
                output.printf("No card detected within %d seconds.%n", cardWaitTimeout.toSeconds());
                return 2;
            }

            try (CardSession connection = readerService.connect(readerName)) {
                output.printf("Reader     : %s%n", connection.readerName());
                output.printf("ATR        : %s%n", HexUtils.toHex(connection.atr()));
                output.printf("Protocol   : %s%n", connection.protocol());
                output.println("Connection : SUCCESS");
                output.println();
                output.println("Available operations:");
                output.println("[0] Connection diagnostic only");
                output.println("[1] SELECT PPSE - Discover payment applications");
                output.print("Select operation: ");

                if (!input.hasNextLine()) {
                    output.println();
                    output.println("Operation selection was not provided.");
                    return 1;
                }

                Integer operation = parseSelection(input.nextLine(), 2);
                if (operation == null) {
                    output.println("Invalid operation selection.");
                    return 1;
                }
                if (operation == 0) {
                    output.println("No APDU command was sent.");
                    return 0;
                }

                executeSelectPpse(connection);
                return 0;
            }
        } catch (CardException error) {
            output.println("Connection : FAILED");
            output.println("Error      : " + error.getMessage());
            if (error.getCause() != null) {
                output.println("Cause      : " + error.getCause().getMessage());
            }
            return 3;
        }
    }

    private void executeSelectPpse(CardSession connection) throws CardException {
        ApduCommand command = EmvCommands.selectPpse();
        long startedAt = System.nanoTime();
        ApduResponse response = connection.transmit(command);
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;

        output.println();
        output.println("Command       : SELECT PPSE");
        output.printf("APDU          : %s%n", HexUtils.toHex(command.bytes()));
        output.printf("Raw Response  : %s%n", HexUtils.toHex(response.bytes()));
        output.printf("Response Data : %s%n",
                response.data().length == 0 ? "<empty>" : HexUtils.toHex(response.data()));
        output.printf("SW1           : %02X%n", response.sw1());
        output.printf("SW2           : %02X%n", response.sw2());
        output.printf("Status Word   : %04X%n", response.statusWord());
        output.printf("Status        : %s%n", StatusWord.describe(response.statusWord()));
        output.printf("Duration      : %d ms%n", durationMillis);
    }

    private static Integer parseSelection(String text, int readerCount) {
        try {
            int selection = Integer.parseInt(text.trim());
            return selection >= 0 && selection < readerCount ? selection : null;
        } catch (NumberFormatException error) {
            return null;
        }
    }
}
