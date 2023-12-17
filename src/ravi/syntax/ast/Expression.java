package ravi.syntax.ast;

import java.util.List;

public sealed interface Expression {

    record UnitExpr() implements Expression { }

    record Application(Expression expr, List<Expression> args) implements Expression { }

    record Instr(Expression primary, Expression result) implements Expression {}

    record LetIn(Identifier.Lowercase valueName, Parameters parameters, Expression expr, Expression result) implements Expression { }

    record GroupExpr(Expression expr) implements Expression { }

    record ParenthesisExpr(Expression expr) implements Expression { }

    record ListExpr(RaviList list) implements Expression { }

    record ConstantExpr(Constant constant) implements Expression { }

    record ValueNameExpr(Identifier.Lowercase valueName) implements Expression { }

    record ConsCell(Expression head, Expression tail) implements Expression { }

    record PatternMatching(Expression expression,
                           List<Pattern> patterns,
                           List<Expression> expressions)
            implements Expression { }

    record Lambda(Parameters parameters, Expression expression) implements Expression { }

    record ModuleCallExpr(Identifier.Capitalized moduleName, Identifier.Lowercase valueName) implements Expression { }

}