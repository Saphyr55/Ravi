package ravi.core;

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


    public static void loop(int n, Runnable next) {
        for (int i = 0; i < n; i++) {
            next.run();
        }
    }
}
