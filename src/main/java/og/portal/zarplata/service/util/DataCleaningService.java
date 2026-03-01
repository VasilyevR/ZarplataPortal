package og.portal.zarplata.service.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DataCleaningService {
    private DataCleaningService() {
        throw new IllegalCallerException("DataCleaningService class cannot be instantiated");
    }

    public static String getLastDigits(String text) {
        if (text == null) {
            return "";
        }
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(text);

        String lastMatch = "";
        while (matcher.find()) {
            lastMatch = matcher.group();
        }

        return lastMatch;
    }

    public static String getDigits(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\D", "");
    }

    public static String trimNonDigits(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("^\\D+|\\D+$", "");
    }

    public static String extractLogin(String fullUsername) {
        if (fullUsername == null) {
            return null;
        }
        int backslashIndex = fullUsername.lastIndexOf('\\');
        if (backslashIndex != -1) {
            return fullUsername.substring(backslashIndex + 1);
        }
        return fullUsername;
    }

    public static String formatCurrency(String value) {
        try {
            if (value == null || value.isEmpty()) return "";

            String cleanValue = value.replaceAll("[^\\d.,]", "").replace(",", ".");
            if (cleanValue.isEmpty()) return value;

            BigDecimal amount = new BigDecimal(cleanValue);

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("ru", "RU"));
            symbols.setGroupingSeparator(' ');
            symbols.setDecimalSeparator(',');

            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", symbols);
            return decimalFormat.format(amount) + "р.";
        } catch (Exception e) {
            return value;
        }
    }
}
