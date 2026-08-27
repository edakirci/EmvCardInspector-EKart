package com.emvcardinspector.reader;

import com.emvcardinspector.apdu.ApduTransport;

import javax.smartcardio.CardException;

/** Active card session capable of exchanging APDUs and exposing connection metadata. */
public interface CardSession extends ApduTransport, AutoCloseable {
    String readerName();

    byte[] atr();

    String protocol();

    @Override
    void close() throws CardException;
}
