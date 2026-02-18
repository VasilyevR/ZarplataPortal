package og.portal.zarplata.service.util;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DataCleaningService {

    public String getLastDigits(String text) {
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

    public String getDigits(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\D", "");
    }

    public String trimNonDigits(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("^\\D+|\\D+$", "");
    }
}
