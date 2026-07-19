package com.telcocrm.paymentservice.util;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public final class CardValidator {

    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("MM/yy");

    private CardValidator() {
    }

    public static boolean isValidLuhn(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }
        String digitsOnly = cardNumber.replaceAll("\\s+", "");
        if (!digitsOnly.matches("\\d{13,19}")) {
            return false;
        }

        int sum = 0;
        boolean doubleDigit = false;
        for (int i = digitsOnly.length() - 1; i >= 0; i--) {
            int digit = digitsOnly.charAt(i) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    public static boolean isExpiryValid(String expiryDate) {
        if (expiryDate == null) {
            return false;
        }
        try {
            YearMonth expiry = YearMonth.parse(expiryDate, EXPIRY_FORMAT);
            return !expiry.isBefore(YearMonth.now());
        } catch (Exception e) {
            return false;
        }
    }
}
