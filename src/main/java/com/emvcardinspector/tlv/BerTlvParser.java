package com.emvcardinspector.tlv;

import com.emvcardinspector.util.HexUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Parses the definite-length BER-TLV encoding used by EMV responses. */
public final class BerTlvParser {
    private static final int MAX_TAG_BYTES = 3;
    private static final int MAX_LENGTH_BYTES = 4;

    public List<TlvNode> parse(byte[] encoded) {
        Objects.requireNonNull(encoded, "encoded");
        return parseRange(encoded, new Cursor(0), encoded.length);
    }

    private List<TlvNode> parseRange(byte[] encoded, Cursor cursor, int endOffset) {
        List<TlvNode> nodes = new ArrayList<>();
        while (cursor.offset < endOffset) {
            skipPadding(encoded, cursor, endOffset);
            if (cursor.offset == endOffset) {
                break;
            }
            nodes.add(parseNode(encoded, cursor, endOffset));
        }
        return List.copyOf(nodes);
    }

    private void skipPadding(byte[] encoded, Cursor cursor, int endOffset) {
        while (cursor.offset < endOffset) {
            int nextByte = encoded[cursor.offset] & 0xFF;
            if (nextByte != 0x00 && nextByte != 0xFF) {
                return;
            }
            cursor.offset++;
        }
    }

    private TlvNode parseNode(byte[] encoded, Cursor cursor, int endOffset) {
        int nodeOffset = cursor.offset;
        int firstTagByte = readByte(encoded, cursor, endOffset, "Missing tag");
        int tagByteCount = 1;

        if ((firstTagByte & 0x1F) == 0x1F) {
            int nextTagByte;
            do {
                if (tagByteCount == MAX_TAG_BYTES) {
                    throw new TlvParseException("EMV tag exceeds three bytes", cursor.offset);
                }
                nextTagByte = readByte(encoded, cursor, endOffset, "Incomplete multi-byte tag");
                tagByteCount++;
            } while ((nextTagByte & 0x80) != 0);
        }

        String tagHex = HexUtils.toHex(Arrays.copyOfRange(
                encoded,
                nodeOffset,
                nodeOffset + tagByteCount));
        boolean constructed = (firstTagByte & 0x20) != 0;
        int length = readLength(encoded, cursor, endOffset);
        int valueOffset = cursor.offset;

        if (length > endOffset - valueOffset) {
            throw new TlvParseException(
                    "Value length " + length + " exceeds available bytes",
                    valueOffset);
        }

        int valueEndOffset = valueOffset + length;
        byte[] value = Arrays.copyOfRange(encoded, valueOffset, valueEndOffset);
        List<TlvNode> children = constructed
                ? parseRange(encoded, new Cursor(valueOffset), valueEndOffset)
                : List.of();
        cursor.offset = valueEndOffset;

        return new TlvNode(
                new TlvTag(tagHex, constructed),
                length,
                value,
                nodeOffset,
                children);
    }

    private int readLength(byte[] encoded, Cursor cursor, int endOffset) {
        int lengthOffset = cursor.offset;
        int firstLengthByte = readByte(encoded, cursor, endOffset, "Missing length");
        if ((firstLengthByte & 0x80) == 0) {
            return firstLengthByte;
        }

        int lengthByteCount = firstLengthByte & 0x7F;
        if (lengthByteCount == 0) {
            throw new TlvParseException("Indefinite length is not supported", lengthOffset);
        }
        if (lengthByteCount > MAX_LENGTH_BYTES) {
            throw new TlvParseException("Length field exceeds four bytes", lengthOffset);
        }
        if (lengthByteCount > endOffset - cursor.offset) {
            throw new TlvParseException("Incomplete long-form length", cursor.offset);
        }

        long length = 0;
        for (int index = 0; index < lengthByteCount; index++) {
            length = (length << 8) | readByte(encoded, cursor, endOffset, "Incomplete length");
        }
        if (length > Integer.MAX_VALUE) {
            throw new TlvParseException("Value length is too large", lengthOffset);
        }
        return (int) length;
    }

    private int readByte(byte[] encoded, Cursor cursor, int endOffset, String message) {
        if (cursor.offset >= endOffset) {
            throw new TlvParseException(message, cursor.offset);
        }
        return encoded[cursor.offset++] & 0xFF;
    }

    private static final class Cursor {
        private int offset;

        private Cursor(int offset) {
            this.offset = offset;
        }
    }
}
