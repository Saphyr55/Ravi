package ravi.syntax.ast;

import java.util.HashMap;
import java.util.List;

public sealed interface Expression {

    record UnitExpr() implements Expression { }

    record Application(Expression expr, List<Expression> args) implements Expression { }

    record Instr(Expression primary, Expression result) implements Expression {}

    record LetIn(Identifier name, Params params, Expression expr, Expression result) implements Expression { }

    record GroupExpr(Expression expr) implements Expression { }

    record ParenthesisExpr(Expression expr) implements Expression { }

    record ListExpr(RaviList list) implements Expression { }

    record ConstantExpr(Constant constant) implements Expression { }

    record IdentifierExpr(Identifier identifier) implements Expression { }

    record ConsCell(Expression head, Expression tail) implements Expression { }

    record PatternMatching(Expression expression,
                           List<Pattern> patterns,
                           List<Expression> expressions)
            implements Expression { }


}
