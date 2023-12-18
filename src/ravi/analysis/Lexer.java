package ravi.analysis;

import ravi.analysis.ast.Operator;
import ravi.core.BindingManager;
import ravi.core.Core;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import static ravi.analysis.Syntax.Symbol;
import static ravi.analysis.Syntax.isOperator;

public class Lexer {

    static boolean hadError = false;
    private String source;
    private int position;
    private int start;
    private int line;
    private int col;
    private final List<Token> tokens;

    public Lexer() {
        this.tokens = new LinkedList<>();
    }

    public List<Token> scan(String source) {
        this.source = source;
        while (!isAtEnd()) {
            start = position;
            nextToken();
        }

        tokens.add(new Token(Kind.EOF, null, String.valueOf(peek()), line, col));
        if (hadError) return List.of();

        return tokens;
    }

    private void nextToken() {
        final String c = String.valueOf(advance());
        switch (c) {
            case Symbol.Colon -> addToken(match(Symbol.Colon) ? Kind.DoubleColon : Kind.Colon);
            case Symbol.OpenParenthesis -> addCommentOrParenthesis();
            case Symbol.CloseParenthesis -> addToken(Kind.CloseParenthesis);
            case Symbol.OpenSquareBracket -> addToken(Kind.OpenSquareBracket);
            case Symbol.CloseSquareBracket -> addToken(Kind.CloseSquareBracket);
            case Symbol.Comma -> addToken(Kind.Comma);
            case Symbol.Semicolon -> addToken(Kind.Semicolon);
            case Symbol.Dot -> addToken(Kind.Dot);
            case Symbol.DoubleQuote -> addStringToken();
            case Symbol.BackslashN -> { line++; col = 0; }
            case Symbol.Space, Symbol.BackslashR, Symbol.BackslashT -> { }
            default -> addDefaultToken(c);
        }
    }

    private void addCommentOrParenthesis() {
        if (match(Symbol.Asterisk)) {
            while (!peekStr().equals(Symbol.Asterisk) && !peekStr().equals(Symbol.CloseParenthesis)) {
                next();
            }
            Core.loop(2, this::next);
            return;
        }
        addToken(Kind.OpenParenthesis);
    }

    private void addDefaultToken(String c) {
        if (Syntax.isOperator(c)) {
            addOperator(c);
            return;
        }
        if (Character.isDigit(c.charAt(0))){
            addNumberToken();
            return;
        }
        if (Core.isAlpha(c.charAt(0))) {
            addIdentifierToken(c.charAt(0));
            return;
        }
        report("Unexpected character.");
    }

    private void addOperator(String c) {

        if (c.equals(Symbol.Minus)) {
            if (match(Symbol.Greater)) {
                addToken(Kind.Arrow);
                return;
            }
        }

        if (c.equals(Symbol.Equal)) {
            if (!Syntax.isOperator(advanceStr())) {
                addToken(Kind.Equal);
                return;
            }
        }

        if (c.equals(Symbol.Pipe)) {
            if (!Syntax.isOperator(advanceStr())) {
                addToken(Kind.Pipe);
                return;
            }
        }

        while (Syntax.isOperator(peekStr())) {
            next();
        }

        String text = source.substring(start, position);
        addToken(Kind.Operator, text);
    }

    private String advanceStr() {
        return String.valueOf(advance());
    }

    private void addNumberToken() {

        while(Character.isDigit(peek())) {
            next();
        }

        if(peek() == '.' ) {
            next();
            while(Character.isDigit(peek())) {
                next();
            }
            String text = source.substring(start, position);
            addToken(Kind.Float, Float.parseFloat(text));
        }
        else {
            String text = source.substring(start, position);
            addToken(Kind.Int,Integer.parseInt(text));
        }
    }

    private void addIdentifierToken(char c) {
        if (Character.isUpperCase(c)) {
            addIdentifierToken(Kind.CapitalizedIdentifier);
            return;
        }
        addIdentifierToken(Kind.LowercaseIdentifier);
    }

    private void addIdentifierToken(Kind kind) {
        while (Core.isAlphaNumeric(peek())) {
            next();
        }
        String text = source.substring(start, position);
        Kind type = BindingManager.KEYWORDS.get(text);
        addToken(type == null ? kind : type, type == null ? text : null);
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

    private String peekStr() {
        return String.valueOf(peek());
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

    private String currentCharStr() {
        return String.valueOf(currentChar());
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
