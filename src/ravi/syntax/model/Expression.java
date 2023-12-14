package ravi.syntax.model;

import ravi.syntax.Text;

import java.util.List;

public sealed interface Expression {

    record Application(Expression expr, List<Expression> args) implements Expression { }

    record ExprSemicolonExpr(Expression expr, Expression result) implements Expression {}

    record LetIn(Identifier identifier, ArgList list, Expression expr, Expression result) implements Expression { }

    record GroupExpr(Expression expr) implements Expression { }

    record ParenthesisExpr(Expression expr) implements Expression { }

    record TextExpr(Text text) implements Expression { }

    record ListExpr(RaviList list) implements Expression { }

    record NumberExpr(RaviNumber number) implements Expression { }

    record IdentifierExpr(Identifier identifier) implements Expression { }

}
