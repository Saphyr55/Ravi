package ravi.syntax;

import ravi.syntax.model.*;

import java.util.LinkedList;
import java.util.List;

public class Parser {

    private final List<Token> tokens;
    private int position;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Program -> Statement Program | epsilon
     *
     * @return
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
        Expression expression = expression();
        while (check(Kind.Semicolon)) {
            consume(Kind.Semicolon, "");
            expressions.add(expression);
            expression = expression();
        }
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

        Expression primary = application();

        return primary;
    }

    private Expression application() {

        Expression primary = expressionPrime();
        Expression expression = expressionPrime();

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

    private Expression expressionPrime() {
        if (check(Kind.Text)) return new Expression.TextExpr(text());
        // if (check(Kind.LetKw)) return letIn();
        if (check(Kind.BeginKw)) return groupExpr();
        if (check(Kind.OpenParenthesis)) return parenthesisExpr();
        if (check(Kind.Identifier)) return new Expression.IdentifierExpr(identifier());
        if (check(Kind.OpenSquareBracket)) return listExpr();
        return null;
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
            consume(Kind.CloseSquareBracket,"");
            return null;
        }
        consume(Kind.Semicolon, "We need a ';' symbol.");
        Expression expr = expression();
        return new RaviRestList(expr,restList());
    }

    private Expression parenthesisExpr() {
        consume(Kind.OpenParenthesis, "We need a '(' symbol.");
        Expression expression = expression();
        consume(Kind.CloseParenthesis, "We need a ')' symbol.");
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
        consume(Kind.EndKw, "We need the 'end' keyword to close a let declarations.");

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

    private Text text() {
        Token text = consume(Kind.Text, "We need a bloc text.");
        return new Text((String) text.value());
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
