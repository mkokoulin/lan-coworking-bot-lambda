package com.lan.app.flows.registration;

import java.util.regex.Pattern;

final class PhoneValidator {

    private static final Pattern AM_PATTERN = Pattern.compile(
            "^\\+?374(10|11|33|41|43|44|49|55|77|91|93|94|95|96|98|99)\\d{6}$"
    );

    static String normalize(String input) {
        if (input == null) return null;
        String digits = input.replaceAll("[\\s\\-()]", "");

        if (digits.startsWith("+374")) {
            
        } else if (digits.startsWith("374")) {
            digits = "+" + digits;
        } else if (digits.startsWith("0") && digits.length() == 9) {
            digits = "+374" + digits.substring(1);
        } else if (digits.length() == 8) {
            digits = "+374" + digits;
        } else {
            return null;
        }

        return AM_PATTERN.matcher(digits).matches() ? digits : null;
    }

    static String lastFour(String normalized) {
        if (normalized == null || normalized.length() < 4) return "";
        return normalized.substring(normalized.length() - 4);
    }

    private PhoneValidator() {}
}
