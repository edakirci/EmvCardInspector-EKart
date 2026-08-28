package com.emvcardinspector.emv;

import com.emvcardinspector.util.HexUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentSchemeTest {
    @Test
    void identifiesPaymentSchemeFromAidRid() {
        assertEquals(PaymentScheme.VISA, PaymentScheme.fromAid(HexUtils.fromHex("A0000000031010")));
        assertEquals(PaymentScheme.MASTERCARD, PaymentScheme.fromAid(HexUtils.fromHex("A0000000041010")));
        assertEquals(PaymentScheme.AMERICAN_EXPRESS, PaymentScheme.fromAid(HexUtils.fromHex("A00000002501")));
        assertEquals(PaymentScheme.UNIONPAY, PaymentScheme.fromAid(HexUtils.fromHex("A000000333010101")));
        assertEquals(PaymentScheme.TROY, PaymentScheme.fromAid(HexUtils.fromHex("A0000006723020")));
    }

    @Test
    void reportsUnknownSchemeForUnregisteredAid() {
        assertEquals(PaymentScheme.UNKNOWN, PaymentScheme.fromAid(HexUtils.fromHex("F000000001")));
    }
}
