package og.portal.zarplata.service.util;

import java.util.Comparator;

public final class WindowsExplorerComparator implements Comparator<String> {

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

            if (c1 == '-' || c1 == '\'') {
                i1++;
                continue;
            }
            if (c2 == '-' || c2 == '\'') {
                i2++;
                continue;
            }

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
                char u1 = Character.toUpperCase(c1);
                char u2 = Character.toUpperCase(c2);

                if (u1 != u2) {
                    return u1 - u2;
                }
                i1++;
                i2++;
            }
        }

        if (i1 >= len1 && i2 >= len2) {
             return s1.compareTo(s2);
        }

        if (i1 >= len1) return -1;
        if (i2 >= len2) return 1;

        return 0;
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
