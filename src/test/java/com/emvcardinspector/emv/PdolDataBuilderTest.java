package com.emvcardinspector.emv;

import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdolDataBuilderTest {
    private final PdolDataBuilder builder = new PdolDataBuilder(
            Clock.fixed(Instant.parse("2026-08-31T12:00:00Z"), ZoneOffset.UTC),
            new PredictableSecureRandom());

    @Test
    void laysOutKnownAndUnknownValuesInPdolOrder() {
        byte[] data = builder.build("9F66049F02069F1A029A039C019F3704DF0102");

        assertEquals(
                "26000000"
                        + "000000000000"
                        + "0792"
                        + "260831"
                        + "00"
                        + "01020304"
                        + "0000",
                HexUtils.toHex(data));
    }

    @Test
    void rejectsPdolTagWithoutLength() {
        assertThrows(EmvDataException.class, () -> builder.build("9F66"));
    }

    private static final class PredictableSecureRandom extends SecureRandom {
        @Override
        public void nextBytes(byte[] bytes) {
            for (int index = 0; index < bytes.length; index++) {
                bytes[index] = (byte) (index + 1);
            }
        }
    }
}
