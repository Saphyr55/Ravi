package ravi.syntax.ast;


import java.util.List;

public sealed interface Statement {

    record Let(Identifier name, Params params, Expression result) implements Statement { }

    record Instr(List<Expression> expression) implements Statement { }

}
