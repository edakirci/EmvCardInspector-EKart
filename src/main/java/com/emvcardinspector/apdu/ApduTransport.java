package com.emvcardinspector.apdu;

import javax.smartcardio.CardException;

/** Boundary used by real PC/SC and future fake test transports. */
@FunctionalInterface
public interface ApduTransport {
    ApduResponse transmit(ApduCommand command) throws CardException;
}
