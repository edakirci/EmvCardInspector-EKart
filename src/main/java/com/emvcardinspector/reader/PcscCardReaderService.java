package com.emvcardinspector.reader;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import java.util.List;

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
    public CardConnection connect(String readerName) throws CardException {
        CardTerminal terminal = terminalFactory.terminals().list().stream()
                .filter(candidate -> candidate.getName().equals(readerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Reader not found: " + readerName));

        return new CardConnection(readerName, terminal.connect("*"));
    }
}
