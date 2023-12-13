package ravi.syntax;

import ravi.syntax.model.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Parser {

    private final List<Token> tokens;
    private int position;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Program -> Instruction Program | epsilon
     *
     * @return
     */
    public Program program() {
        Instruction instruction = instruction();
        Program program = isAtEnd() ? null : program();
        return new Program(instruction, program);
    }

    private Instruction instruction() {
        if (check(Kind.LetKw)) return let();
        return instructionExpr();
    }

    private Instruction instructionExpr() {
        Expression expression = expression();
        return new Instruction.Expr(expression);
    }

    private Instruction let() {

        consume(Kind.LetKw, "We need the 'let' keyword.");
        Identifier identifier = identifier();
        ArgList argList = argList();

        consume(Kind.Equal, "We need the '=' symbol.");

        Expression result = expression();
        consume(Kind.EndKw, "We need the 'end' keyword to close a let declarations.");

        return new Instruction.Let(identifier, argList, result);
    }

    private Expression expression() {

        Expression in = expressionPrime();

        if (check(Kind.Semicolon)) {
            consume(Kind.Semicolon, "");
            Expression expression = expression();
            if (expression == null)
            return new Expression.ExprSemicolonExpr(in, expression);
        }

        return application(in);
    }

    private Expression expressionPrime() {
        if (check(Kind.Text)) return new Expression.TextExpr(text());
        if (check(Kind.LetKw)) return letIn();
        if (check(Kind.BeginKw)) return groupExpr();
        if (check(Kind.OpenParenthesis)) return parenthesisExpr();
        if (check(Kind.Identifier)) return new Expression.IdentifierExpr(identifier());
        return null;
    }

    private Expression application(Expression in) {

        List<Expression> expressions = new ArrayList<>();
        Expression expression = expressionPrime();

        if (expression == null) {
            return in;
        }

        while (expression != null) {
            expressions.add(expression);
            expression = expression();
        }

        return new Expression.Application(in, expressions);
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
        ArgList argList = argList();

        consume(Kind.Equal, "We need the '=' symbol.");

        Expression resultLet = expression();
        consume(Kind.InKw, "We need the 'in' keyword to close a let declarations.");

        Expression resultIn = expression();
        consume(Kind.EndKw, "We need the 'end' keyword to close a let declarations.");

        return new Expression.LetIn(identifier, argList, resultLet, resultIn);
    }

    private ArgList argList() {
        List<Identifier> identifiers = new LinkedList<>();
        while (check(Kind.Identifier)) {
            Token token = consume(Kind.Identifier, "We need an identifier.");
            Identifier identifier = new Identifier((String) token.value());
            identifiers.add(identifier);
        }
        return new ArgList(identifiers);
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
