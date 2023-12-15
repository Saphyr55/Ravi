package ravi.syntax;

public class TextParsing {

    public static String parse(String text) {
        text = text.strip();
        text = text.substring(3, text.length() - 3).stripIndent();
        text = text.substring(1).stripTrailing();
        return text;
    }

}
