package com.emvcardinspector.emv;

import com.emvcardinspector.util.HexUtils;

import java.util.Arrays;

/** Payment scheme identified from the five-byte registered AID prefix (RID). */
public enum PaymentScheme {
    VISA("A000000003", "Visa"),
    MASTERCARD("A000000004", "Mastercard"),
    AMERICAN_EXPRESS("A000000025", "American Express"),
    JCB("A000000065", "JCB"),
    DISCOVER("A000000152", "Discover"),
    UNIONPAY("A000000333", "UnionPay"),
    TROY("A000000672", "TROY"),
    UNKNOWN("", "Unknown");

    private final String rid;
    private final String displayName;

    PaymentScheme(String rid, String displayName) {
        this.rid = rid;
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static PaymentScheme fromAid(byte[] aid) {
        if (aid == null || aid.length < 5) {
            return UNKNOWN;
        }
        String rid = HexUtils.toHex(Arrays.copyOf(aid, 5));
        return Arrays.stream(values())
                .filter(scheme -> !scheme.rid.isEmpty() && scheme.rid.equals(rid))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
