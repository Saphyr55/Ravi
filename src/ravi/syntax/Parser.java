package ravi.syntax;

import ravi.syntax.ast.*;

import java.util.*;

public class Parser {

    private final List<Token> tokens;
    private int position;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Program -> Statement Program | epsilon
     *
     * @return program
     */
    public Program program() {
        Statement statement = statement();
        Program program = isAtEnd() ? null : program();
        return new Program(statement, program);
    }

    private Statement statement() {
        if (check(Kind.LetKw)) return let();
        return instructionExpr();
    }

    private Statement instructionExpr() {
        List<Expression> expressions = new LinkedList<>();
        expressions.add(expression());
        do {
            expressions.add(expression());
            consume(Kind.Semicolon, "We need a ';' symbole to close the instruction.");
        } while (check(Kind.Semicolon));
        return new Statement.Instr(expressions);
    }

    private Statement let() {

        consume(Kind.LetKw, "We need the 'let' keyword.");
        Identifier identifier = identifier();
        Params params = params();

        consume(Kind.Equal, "We need the '=' symbol.");

        Expression result = expression();
        consume(Kind.EndKw, "We need the 'end' keyword to close a let declarations.");

        return new Statement.Let(identifier, params, result);
    }

    private Expression expression() {
        return application();
    }

    private Expression application() {

        Expression primary = expressionPrime();
        Expression expression = consCell(primary);

        if (expression == null) {
            return primary;
        }

        List<Expression> expressions = new LinkedList<>();

        while (expression != null) {
            expressions.add(expression);
            expression = expression();
        }

        return new Expression.Application(primary, expressions);
    }

    private Expression consCell(Expression head) {
        if (check(Kind.DoubleColon)) {
            consume(Kind.DoubleColon, "");
            Expression tail = expression();
            return new Expression.ConsCell(head, tail);
        }
        return expressionPrime();
    }

    private Expression expressionPrime() {
        if (check(Kind.String)) return stringExpr();
        if (check(Kind.Text)) return textExpr();
        if (check(Kind.LetKw)) return letIn();
        if (check(Kind.BeginKw)) return groupExpr();
        if (check(Kind.OpenParenthesis)) return parenthesisExpr();
        if (check(Kind.Identifier)) return new Expression.IdentifierExpr(identifier());
        if (check(Kind.OpenSquareBracket)) return listExpr();
        if (check(Kind.MatchKw)) return patternMatching();
        return null;
    }

    private Expression patternMatching() {
        consume(Kind.MatchKw, "We need the keyword 'match' to declare a pattern matching.");
        Expression expression = expression();
        consume(Kind.WithKw, "We need the keyword 'with' after an expression.");
        List<Pattern> patterns = new ArrayList<>();
        List<Expression> expressions = new ArrayList<>();
        while (check(Kind.Pipe)) {
            consume(Kind.Pipe, "");
            patterns.add(pattern());
            consume(Kind.Arrow, "We need the '->' to result an expression.");
            expressions.add(expression());
        }
        return new Expression.PatternMatching(expression, patterns, expressions);
    }

    private Expression textExpr() {
        return new Expression.ConstantExpr(text());
    }

    private Expression stringExpr() {
        return new Expression.ConstantExpr(string());
    }

    private Expression listExpr() {
        consume(Kind.OpenSquareBracket, "We need a '[' symbol.");
        if (check((Kind.CloseSquareBracket))){
            consume(Kind.CloseSquareBracket,"");
            return new Expression.ListExpr(new RaviList.EmptyList());
        }
        Expression expression = expression();
        RaviRestList rest = restList();
        consume(Kind.CloseSquareBracket, "We need a ']' symbol.");
        return new Expression.ListExpr(new RaviList.List(expression,rest));
    }

    private RaviRestList restList() {
        if (check((Kind.CloseSquareBracket))){
            return null;
        }
        consume(Kind.Semicolon, "We need a ';' symbol.");
        Expression expr = expression();
        return new RaviRestList(expr, restList());
    }

    private Expression parenthesisExpr() {
        consume(Kind.OpenParenthesis, "We need a '(' symbol.");
        Expression expression = expression();
        consume(Kind.CloseParenthesis, "We need a ')' symbol.");
        if (expression == null) return new Expression.UnitExpr();
        return new Expression.ParenthesisExpr(expression);
    }

    private Expression groupExpr() {
        consume(Kind.BeginKw, "We need the 'begin' keyword.");
        Expression expression = expression();
        consume(Kind.EndKw, "We need the 'end' keyword.");
        return expression;
    }

    private Expression letIn() {

        consume(Kind.LetKw, "We need the 'let' keyword.");
        Identifier identifier = identifier();
        Params params = params();

        consume(Kind.Equal, "We need the '=' symbol.");

        Expression resultLet = expression();
        consume(Kind.InKw, "We need the 'in' keyword to close a let declarations.");
        Expression resultIn = expression();

        return new Expression.LetIn(identifier, params, resultLet, resultIn);
    }

    private Params params() {
        List<Identifier> identifiers = new LinkedList<>();
        while (check(Kind.Identifier)) {
            Token token = consume(Kind.Identifier, "We need an identifier.");
            Identifier identifier = new Identifier((String) token.value());
            identifiers.add(identifier);
        }
        return new Params(identifiers);
    }

    private Pattern pattern() {
        Pattern primary = patternPrime();
        Pattern pattern = consCellPattern(primary);
        if (pattern == null) {
            return primary;
        }
        return pattern;
    }

    private Pattern consCellPattern(Pattern head) {
        if (check(Kind.DoubleColon)) {
            consume(Kind.DoubleColon, "");
            Pattern tail = pattern();
            return new Pattern.PCons(head, tail);
        }
        return patternPrime();
    }

    private Pattern patternPrime() {
        if (check(Kind.Identifier)) return identifierPattern();
        if (check(Kind.OpenParenthesis)) return groupPattern();
        Constant constant = constant();
        if (constant == null) return null;
        return new Pattern.PConstant(constant);
    }

    private Pattern groupPattern() {
        consume(Kind.OpenParenthesis, "We need a '(' to open a group pattern.");
        Pattern pattern = pattern();
        consume(Kind.CloseParenthesis, "We need a ')' to close the group pattern.");
        return pattern;
    }

    private Pattern identifierPattern() {
        Identifier identifier = identifier();
        if (identifier.name().equals("_")) {
            return new Pattern.PAny();
        }
        return new Pattern.PIdentifier(identifier);
    }

    private Constant constant() {
        if (check(Kind.OpenSquareBracket)) return emptyListConstant();
        if (check(Kind.Text)) return text();
        if (check(Kind.String)) return string();
        return null;
    }

    private Constant emptyListConstant() {
        consume(Kind.OpenSquareBracket, "We need a '[' symbol.");
        consume(Kind.CloseSquareBracket,"We need a ']' symbol.");
        return new Constant.CEmptyList();
    }

    private Constant text() {
        Token token = consume(Kind.Text, "We need a bloc text.");
        return new Constant.CText((String) token.value());
    }

    private Constant string() {
        Token token = consume(Kind.String, "We need a string.");
        return new Constant.CString((String) token.value());
    }

    private Identifier identifier() {
        Token identifier = consume(Kind.Identifier, "We need an identifier.");
        return new Identifier((String) identifier.value());
    }

    private Token consume(Kind kind, String message) {
        if (check(kind)) {
            return nextToken();
        }
        throw new RuntimeException(message);
    }

    private boolean isAtEnd() {
        return check(Kind.EOF);
    }

    private Token nextToken() {

        if (position >= tokens.size()) {
            return null;
        }

        Token token = tokens.get(position);
        position++;
        return token;
    }

    private boolean check(Kind kind) {
        return currentToken().kind() == kind;
    }

    private Token currentToken() {
        return tokens.get(position);
    }

}
