package com.emvcardinspector.reader;

import javax.smartcardio.CardException;
import java.util.List;

/** Discovers PC/SC readers and opens read-only card connections. */
public interface CardReaderService {
    List<String> listReaderNames() throws CardException;

    CardConnection connect(String readerName) throws CardException;
}
