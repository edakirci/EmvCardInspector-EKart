package com.emvcardinspector.reader;

import javax.smartcardio.CardException;
import java.time.Duration;
import java.util.List;

/** Discovers PC/SC readers and opens read-only card connections. */
public interface CardReaderService {
    List<String> listReaderNames() throws CardException;

    boolean waitForCard(String readerName, Duration timeout) throws CardException;

    CardConnection connect(String readerName) throws CardException;
}
