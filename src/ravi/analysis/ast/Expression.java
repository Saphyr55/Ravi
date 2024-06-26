package ravi.analysis.ast;

import java.util.List;

public sealed interface Expression {

    record UnitExpr() implements Expression { }

    record Application(Expression expr, List<Expression> args) implements Expression { }

    record ApplicationOperator(Expression left, String op, Expression right) implements Expression { }

    record Instr(Expression primary, Expression result) implements Expression {}

    record LetIn(Nameable.ValueName name, Parameters parameters, Expression expr, Expression result) implements Expression { }

    record GroupExpr(Expression expr) implements Expression { }

    record ParenthesisExpr(Expression expr) implements Expression { }

    record ListExpr(RaviList list) implements Expression { }

    record ConstantExpr(Constant constant) implements Expression { }

    record ConsCell(Expression head, Expression tail) implements Expression { }

    record PatternMatching(Expression expression,
                           List<Pattern> patterns,
                           List<Expression> expressions)
            implements Expression { }


    record IfExpr(Expression condition,
                  Expression exprIf,
                  Expression exprElse)
            implements Expression { }

    record Lambda(Parameters parameters, Expression expr) implements Expression { }

    record ModuleCallExpr(Nameable.ModuleName moduleName,
                          Nameable.ValueName valueName)
            implements Expression { }

    record IdentExpr(Nameable.ValueName valueName) implements Expression { }

    record Unary(Operator operator, Expression right) implements Expression { }

    record Binary(Expression left, Operator operator, Expression right) implements Expression  { }

    record Tuple(List<Expression> expressions) implements Expression { }

}
