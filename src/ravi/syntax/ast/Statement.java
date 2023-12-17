package ravi.syntax.ast;


import java.util.List;

public sealed interface Statement {

    record Let(Identifier.Lowercase name, Parameters parameters, Expression result) implements Statement { }

    record Instr(List<Expression> expression) implements Statement { }

    record Module(Identifier.Capitalized moduleName, ModuleContent moduleContent) implements Statement { }

}
