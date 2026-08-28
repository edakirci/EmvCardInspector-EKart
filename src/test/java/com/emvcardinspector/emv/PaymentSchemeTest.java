package com.emvcardinspector.emv;

import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentSchemeTest {
    @Test
    void identifiesPaymentSchemeFromAidRid() {
        assertEquals(PaymentScheme.VISA, PaymentScheme.fromAid(HexUtils.fromHex("A0000000031010")));
        assertEquals(PaymentScheme.MASTERCARD, PaymentScheme.fromAid(HexUtils.fromHex("A0000000041010")));
    }

    @Test
    void reportsUnknownSchemeForUnregisteredAid() {
        assertEquals(PaymentScheme.UNKNOWN, PaymentScheme.fromAid(HexUtils.fromHex("F000000001")));
    }
}
