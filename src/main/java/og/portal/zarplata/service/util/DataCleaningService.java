package og.portal.zarplata.service.util;

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
}
