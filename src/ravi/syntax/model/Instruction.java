package ravi.syntax.model;


public sealed interface Instruction {

    record Let(Identifier name, ArgList argList, Expression result) implements Instruction { }

    record Expr(Expression expression) implements Instruction { }

}
