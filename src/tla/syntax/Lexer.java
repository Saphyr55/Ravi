package tla.syntax;

import tla.core.BindingManager;
import tla.core.Core;

import java.util.List;
import java.util.function.Supplier;

import static tla.syntax.Syntax.Symbol;

public class Lexer {

    static boolean hadError = false;
    private final String text;
    private int position;
    private int start;
    private int line;
    private int col;
    private final List<Token> tokens;

    public Lexer(String text, Supplier<List<Token>> listSupplier) {
        this.tokens = listSupplier.get();
        this.text = text;
    }

    public List<Token> scan() {

        while (!isAtEnd()) {
            start = position;
            nextToken();
        }

        tokens.add(new Token(Kind.EOF, null, String.valueOf(peek()), line, col));

        return tokens;
    }

    private void nextToken() {
        final String s = String.valueOf(advance());
        switch (s) {
            case Symbol.DoubleQuote -> createTextToken();
            case Symbol.OpenSquareBracket -> addToken(Kind.OpenSquareBracket);
            case Symbol.CloseSquareBracket -> addToken(Kind.CloseSquareBracket);
            case Symbol.Equal -> addToken(Kind.Equal);
            case Symbol.Comma -> addToken(Kind.Comma);
            case Symbol.BackslashN -> { line++; col = 0; }
            case Symbol.Space, Symbol.BackslashR, Symbol.BackslashT -> { }
            default -> addDefaultToken(s);
        }
    }

    private void addDefaultToken(String c) {
        if (Core.isAlpha(c.charAt(0))) {
            addIdentifierToken();
            return;
        }
        report("Unexpected character.");
    }

    private void addIdentifierToken() {

        while (Core.isAlphaNumeric(peek())) {
            next();
        }

        Kind type = BindingManager.KEYWORDS.get(text.substring(start, position));
        addToken(type == null ? Kind.Identifier : type);
    }

    private void createTextToken() {
        if (match(Symbol.DoubleQuote) && match(Symbol.DoubleQuote)) {

            while (peek() != Symbol.DoubleQuote.charAt(0) && !isAtEnd()) {
                if (peek() == '\n') line++;
                next();
            }

            if (isAtEnd()) {
                report("Unterminated string.");
                return;
            }

            next();
            next();
            next();

            String value = text.substring(start + 3, position - 3).strip();
            addToken(Kind.Text, value);
            return;
        }
        report("We need three double quote.");
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return text.charAt(position);
    }

    private char peekNext() {

        if (position + 1 >= text.length()) {
            return '\0';
        }

        return text.charAt(position + 1);
    }

    private char advance() {
        return text.charAt(next());
    }

    private int next() {
        col++;
        return position++;
    }

    private int preNext() {
        return ++position;
    }

    private char currentChar() {
        return position >= text.length() ? '\0' : text.charAt(position);
    }

    private boolean isAtEnd() {
        return position >= text.length();
    }

    private void report(String message) {
        System.err.println("Error" + ": " + message + " At " + "[" + ( line + 1 ) + "," + ( col + 1 ) + "].");
        hadError = true;
    }

    private void addToken(Kind kind) {
        addToken(kind, null);
    }

    private void addToken(Kind kind, Object value) {
        String subtext = text.substring(start, position);
        tokens.add(new Token(kind, value, subtext, line, col));
    }

    private boolean match(String expected) {
        if (isAtEnd()) {
            return false;
        }
        if (!String.valueOf(text.charAt(position)).equals(expected)) {
            return false;
        }
        next();
        return true;
    }


}
