package tla.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Core {

    public static String stringMax(String text, int max) {
        return (text.length() > max ? text.substring(0, max) + "..." : text);
    }

    public static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    public static boolean isAlphaNumeric(char c) {
        return Character.isDigit(c) || isAlpha(c);
    }


}
