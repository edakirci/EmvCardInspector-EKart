package com.emvcardinspector.emv;

import com.emvcardinspector.util.HexUtils;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/** Builds terminal data in the tag/length order requested by a card's PDOL. */
public final class PdolDataBuilder {
    private static final DateTimeFormatter EMV_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final Map<String, byte[]> FIXED_TERMINAL_VALUES = Map.of(
            "9F02", HexUtils.fromHex("000000000000"),
            "9F03", HexUtils.fromHex("000000000000"),
            "9F1A", HexUtils.fromHex("0792"),
            "5F2A", HexUtils.fromHex("0949"),
            "9F66", HexUtils.fromHex("26000000"),
            "9C", HexUtils.fromHex("00"),
            "95", HexUtils.fromHex("0000000000"));

    private final Clock clock;
    private final SecureRandom secureRandom;

    public PdolDataBuilder() {
        this(Clock.systemDefaultZone(), new SecureRandom());
    }

    PdolDataBuilder(Clock clock, SecureRandom secureRandom) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
    }

    public byte[] build(String pdolHex) {
        Objects.requireNonNull(pdolHex, "pdolHex");
        return build(HexUtils.fromHex(pdolHex));
    }

    public byte[] build(byte[] pdol) {
        Objects.requireNonNull(pdol, "pdol");
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int offset = 0;
        while (offset < pdol.length) {
            int tagStart = offset;
            int firstTagByte = pdol[offset++] & 0xFF;
            if ((firstTagByte & 0x1F) == 0x1F) {
                boolean finalTagByteFound = false;
                while (offset < pdol.length) {
                    int tagByte = pdol[offset++] & 0xFF;
                    if ((tagByte & 0x80) == 0) {
                        finalTagByteFound = true;
                        break;
                    }
                }
                if (!finalTagByteFound) {
                    throw new EmvDataException("PDOL contains an incomplete multi-byte tag", tagStart);
                }
            }
            if (offset >= pdol.length) {
                throw new EmvDataException("PDOL tag is missing its requested length", tagStart);
            }

            String tag = HexUtils.toHex(Arrays.copyOfRange(pdol, tagStart, offset));
            int requestedLength = pdol[offset++] & 0xFF;
            result.writeBytes(valueFor(tag, requestedLength));
        }
        return result.toByteArray();
    }

    private byte[] valueFor(String tag, int requestedLength) {
        byte[] value;
        if (tag.equals("9A")) {
            value = HexUtils.fromHex(LocalDate.now(clock).format(EMV_DATE));
        } else if (tag.equals("9F37")) {
            value = new byte[requestedLength];
            secureRandom.nextBytes(value);
            return value;
        } else {
            value = FIXED_TERMINAL_VALUES.getOrDefault(tag, new byte[0]);
        }

        return Arrays.copyOf(value, requestedLength);
    }
}
