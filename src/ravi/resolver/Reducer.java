package ravi.resolver;

import ravi.analysis.ast.Expression;
import ravi.analysis.ast.Program;
import ravi.analysis.ast.Statement;

public final class Reducer {

    public Program reduce(Program program) {
        return program(program);
    }

    Program program(Program program) {
        if (program == null) return null;
        Statement statement = statement(program.statement());
        return new Program(statement, program(program.program()));
    }

    Statement statement(Statement statement) {
        if (statement instanceof Statement.Let let) {
            return new Statement.Let(let.name(), let.parameters(), expression(let.result()));
        }
        return statement;
    }

    Expression expression(Expression expression) {
        return expression;
    }


}
