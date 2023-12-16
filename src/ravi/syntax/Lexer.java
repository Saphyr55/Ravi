package ravi.syntax;

import ravi.core.BindingManager;
import ravi.core.Core;

import java.util.List;
import java.util.function.Supplier;

import static ravi.syntax.Syntax.Symbol;

public class Lexer {

    static boolean hadError = false;
    private final String source;
    private int position;
    private int start;
    private int line;
    private int col;
    private final List<Token> tokens;

    public Lexer(String source, Supplier<List<Token>> listSupplier) {
        this.tokens = listSupplier.get();
        this.source = source;
    }

    public List<Token> scan() {

        while (!isAtEnd()) {
            start = position;
            nextToken();
        }

        tokens.add(new Token(Kind.EOF, null, String.valueOf(peek()), line, col));
        if (hadError) return List.of();
        return tokens;
    }

    private void nextToken() {
        final String s = String.valueOf(advance());
        switch (s) {
            case Symbol.DoubleQuote -> addStringToken();
            case Symbol.OpenParenthesis -> addToken(Kind.OpenParenthesis);
            case Symbol.CloseParenthesis -> addToken(Kind.CloseParenthesis);
            case Symbol.OpenSquareBracket -> addToken(Kind.OpenSquareBracket);
            case Symbol.CloseSquareBracket -> addToken(Kind.CloseSquareBracket);
            case Symbol.Equal -> addToken(Kind.Equal);
            case Symbol.Comma -> addToken(Kind.Comma);
            case Symbol.Semicolon -> addToken(Kind.Semicolon);
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

        String text = source.substring(start, position);
        Kind type = BindingManager.KEYWORDS.get(text);
        addToken(type == null ? Kind.Identifier : type, type == null ? text : null);
    }

    private void addStringToken() {
        if (match(Symbol.DoubleQuote)) {
            if (match(Symbol.DoubleQuote)) {
                if (matchNewLine()) {
                    if (passString(() -> line++)) return;
                    Core.loop(3, this::next);

                    String value = TextParsing.parse(source.substring(start, position));
                    addToken(Kind.Text, value);
                    return;
                }
                report("We need to close the text.");
                return;
            }
            addToken(Kind.String, "");
            return;
        }
        if (passString(() -> report("Unterminated string."))) return;
        next();
        addToken(Kind.String, source.substring(start + 1, position - 1));
    }

    private boolean matchNewLine() {
        String str = System.lineSeparator();
        for (int i = 0; i < str.length(); i++) {
            var c = str.charAt(i);
            var cr = currentChar();
            if (cr != c) {
                return false;
            }
            next();
        }
        return true;
    }

    private boolean passString(Runnable runnable) {
        while (peek() != Symbol.DoubleQuote.charAt(0) && !isAtEnd()) {
            if (peek() == '\n') runnable.run();
            next();
        }
        if (isAtEnd()) {
            report("Unterminated string.");
            return true;
        }
        return false;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(position);
    }

    private char advance() {
        return source.charAt(next());
    }

    private int next() {
        col++;
        return position++;
    }

    private int preNext() {
        return ++position;
    }

    private char currentChar() {
        return position >= source.length() ? '\0' : source.charAt(position);
    }

    private boolean isAtEnd() {
        return position >= source.length();
    }

    private void report(String message) {
        System.err.println("Error" + ": " + message + " " + "[" + ( line + 1 ) + "," + ( col + 1 ) + "].");
        hadError = true;
    }

    private void addToken(Kind kind) {
        addToken(kind, null);
    }

    private void addToken(Kind kind, Object value) {
        String subtext = source.substring(start, position);
        tokens.add(new Token(kind, value, subtext, line, col));
    }

    private boolean match(String expected) {
        if (isAtEnd()) {
            return false;
        }
        if (!String.valueOf(currentChar()).equals(expected)) {
            return false;
        }
        next();
        return true;
    }


}
