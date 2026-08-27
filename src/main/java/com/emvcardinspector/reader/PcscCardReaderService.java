package com.emvcardinspector.reader;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** PC/SC implementation backed by the JDK {@code java.smartcardio} module. */
public final class PcscCardReaderService implements CardReaderService {
    private final TerminalFactory terminalFactory;

    public PcscCardReaderService() {
        this(TerminalFactory.getDefault());
    }

    PcscCardReaderService(TerminalFactory terminalFactory) {
        this.terminalFactory = terminalFactory;
    }

    @Override
    public List<String> listReaderNames() throws CardException {
        return terminalFactory.terminals().list().stream()
                .map(CardTerminal::getName)
                .toList();
    }

    @Override
    public boolean waitForCard(String readerName, Duration timeout) throws CardException {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        return findTerminal(readerName).waitForCardPresent(timeout.toMillis());
    }

    @Override
    public CardConnection connect(String readerName) throws CardException {
        return new CardConnection(readerName, findTerminal(readerName).connect("*"));
    }

    private CardTerminal findTerminal(String readerName) throws CardException {
        Objects.requireNonNull(readerName, "readerName");
        return terminalFactory.terminals().list().stream()
                .filter(candidate -> candidate.getName().equals(readerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Reader not found: " + readerName));
    }
}
