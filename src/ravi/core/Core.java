package ravi.core;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Core {

    public static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz".toCharArray();

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

    public static <A, B> Map<A, B> zipToMap(List<A> keys, List<B> values) {
        return IntStream.range(0, Math.min(keys.size(), values.size()))
                .boxed()
                .collect(Collectors.toMap(keys::get, values::get));
    }


}
