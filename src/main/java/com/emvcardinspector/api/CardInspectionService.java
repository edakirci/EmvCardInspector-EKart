package com.emvcardinspector.api;

import com.emvcardinspector.app.ReaderDiagnosticCommand;
import com.emvcardinspector.reader.CardReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

@Service
public final class CardInspectionService {
    private static final Duration DEFAULT_CARD_WAIT_TIMEOUT = Duration.ofSeconds(30);

    private final CardReaderService readerService;
    private final Duration cardWaitTimeout;
    private final ReentrantLock inspectionLock = new ReentrantLock();

    @Autowired
    public CardInspectionService(CardReaderService readerService) {
        this(readerService, DEFAULT_CARD_WAIT_TIMEOUT);
    }

    CardInspectionService(CardReaderService readerService, Duration cardWaitTimeout) {
        this.readerService = Objects.requireNonNull(readerService, "readerService");
        this.cardWaitTimeout = Objects.requireNonNull(cardWaitTimeout, "cardWaitTimeout");
    }

    public CardInspectionResponse inspectContact() {
        return inspect(InspectionInterface.CONTACT);
    }

    public CardInspectionResponse inspectContactless() {
        return inspect(InspectionInterface.CONTACTLESS);
    }

    private CardInspectionResponse inspect(InspectionInterface inspectionInterface) {
        if (!inspectionLock.tryLock()) {
            return new CardInspectionResponse("busy", "Başka bir kart incelemesi devam ediyor.", 0);
        }

        long startedAt = System.nanoTime();
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        try (Scanner unusedInput = new Scanner("");
             PrintStream output = new PrintStream(outputBytes, true, StandardCharsets.UTF_8)) {
            ReaderDiagnosticCommand command = new ReaderDiagnosticCommand(
                    readerService, unusedInput, output, cardWaitTimeout);
            ReaderDiagnosticCommand.SessionStatus status = inspectionInterface.run(command);
            return new CardInspectionResponse(
                    status.name().toLowerCase(Locale.ROOT),
                    outputBytes.toString(StandardCharsets.UTF_8),
                    elapsedMillis(startedAt));
        } finally {
            inspectionLock.unlock();
        }
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private enum InspectionInterface {
        CONTACT {
            @Override
            ReaderDiagnosticCommand.SessionStatus run(ReaderDiagnosticCommand command) {
                return command.runContactSession();
            }
        },
        CONTACTLESS {
            @Override
            ReaderDiagnosticCommand.SessionStatus run(ReaderDiagnosticCommand command) {
                return command.runContactlessSession();
            }
        };

        abstract ReaderDiagnosticCommand.SessionStatus run(ReaderDiagnosticCommand command);
    }
}
