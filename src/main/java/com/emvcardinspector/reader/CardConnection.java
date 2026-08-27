package com.emvcardinspector.reader;

import com.emvcardinspector.apdu.ApduCommand;
import com.emvcardinspector.apdu.ApduResponse;
import com.emvcardinspector.apdu.ApduTransport;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.ResponseAPDU;
import java.util.Objects;

/** Owns a live smart-card connection and its basic APDU channel. */
public final class CardConnection implements ApduTransport, AutoCloseable {
    private final String readerName;
    private final Card card;

    CardConnection(String readerName, Card card) {
        this.readerName = Objects.requireNonNull(readerName, "readerName");
        this.card = Objects.requireNonNull(card, "card");
    }

    public String readerName() {
        return readerName;
    }

    public byte[] atr() {
        return card.getATR().getBytes().clone();
    }

    @Override
    public ApduResponse transmit(ApduCommand command) throws CardException {
        ResponseAPDU response = card.getBasicChannel().transmit(command.toCommandApdu());
        return ApduResponse.from(response);
    }

    @Override
    public void close() throws CardException {
        card.disconnect(false);
    }
}
