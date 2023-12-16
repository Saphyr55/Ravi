package ravi.syntax.model;

import java.util.List;

public sealed interface Expression {

    record UnitExpr() implements Expression { }

    record Application(Expression expr, List<Expression> args) implements Expression { }

    record Instr(Expression primary, Expression result) implements Expression {}

    record LetIn(Identifier name, Params params, Expression expr, Expression result) implements Expression { }

    record GroupExpr(Expression expr) implements Expression { }

    record ParenthesisExpr(Expression expr) implements Expression { }

    record TextExpr(Text text) implements Expression { }

    record StringExpr(String content) implements Expression { }

    record ListExpr(RaviList list) implements Expression { }

    record NumberExpr(RaviNumber number) implements Expression { }

    record IdentifierExpr(Identifier identifier) implements Expression { }

}
