package og.portal.zarplata.service.util;

import java.util.Comparator;

public class WindowsExplorerComparator implements Comparator<String> {

    @Override
    public int compare(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0;
        }

        int len1 = s1.length();
        int len2 = s2.length();
        int i1 = 0;
        int i2 = 0;

        while (i1 < len1 && i2 < len2) {
            char c1 = s1.charAt(i1);
            char c2 = s2.charAt(i2);

            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                String num1Str = extractNumber(s1, i1);
                String num2Str = extractNumber(s2, i2);

                int cmp = compareNumbers(num1Str, num2Str);
                if (cmp != 0) {
                    return cmp;
                }

                i1 += num1Str.length();
                i2 += num2Str.length();
            } else {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);

                if (c1 != c2) {
                    return c1 - c2;
                }
                i1++;
                i2++;
            }
        }

        return len1 - len2;
    }

    private String extractNumber(String s, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else {
                break;
            }
        }
        return sb.toString();
    }

    private int compareNumbers(String n1, String n2) {
        String cleanN1 = n1.replaceFirst("^0+(?!$)", "");
        String cleanN2 = n2.replaceFirst("^0+(?!$)", "");

        if (cleanN1.length() != cleanN2.length()) {
            return cleanN1.length() - cleanN2.length();
        }
        return cleanN1.compareTo(cleanN2);
    }
}
