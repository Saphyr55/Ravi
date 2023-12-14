package ravi.resolver;

import ravi.model.Func;
import ravi.model.Value;
import ravi.syntax.model.Expression;
import ravi.syntax.model.Identifier;
import ravi.syntax.model.Instruction;
import ravi.syntax.model.Program;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter {

    private final Map<Expression, Integer> locals;
    private final Environment globals;
    private Environment environment;

    public Interpreter() {
        this.locals = new HashMap<>();
        this.environment = new Environment();
        this.globals = environment;

        this.globals.define("print", new Value.Func((inter, args) -> {
            System.out.println(args.get(0));
            return null;
        }));

    }

    public void interpretProgram(Program program) {
        if (program != null) {
            interpretInstruction(program.instruction());
            interpretProgram(program.program());
        }
    }

    void interpretInstruction(Instruction instruction) {
        if (instruction instanceof Instruction.Let let) {
            var name = let.name().identifier();
            var params = let.argList()
                    .declarations()
                    .stream()
                    .map(Identifier::identifier)
                    .toList();

            environment.define(name, new Value.Func(new Func(params, let.result(), environment)));
            return;
        }
        if (instruction instanceof Instruction.Expr expr) {
            evaluate(expr.expression());
            return;
        }
    }

    Value evaluate(Expression expression) {

        if (expression instanceof Expression.ParenthesisExpr expr) {
            return evaluate(expr.expr());
        }

        if (expression instanceof Expression.GroupExpr expr) {
            return evaluate(expr.expr());
        }

        if (expression instanceof Expression.TextExpr expr) {
            return new Value.Str(expr.text().content());
        }

        if (expression instanceof Expression.IdentifierExpr expr) {
            return lookUpVariable(expr.identifier().identifier(), expr);
        }

        if (expression instanceof Expression.ExprSemicolonExpr expr) {
            expr.expressions().forEach(this::evaluate);
            var result = expr.result();
            return evaluate(result);
        }

        if (expression instanceof Expression.LetIn expr)  {
            throw new RuntimeException("Not implemented yet.");
        }

        if (expression instanceof Expression.Application application) {

            Value value = evaluate(application.expr());

            List<Value> args = application
                    .args()
                    .stream()
                    .map(this::evaluate)
                    .map(v -> {
                        if (v instanceof Value.Func f && f.application().arity() == 0) {
                            return f.application().apply(this, List.of());
                        }
                        return v;
                    })
                    .toList();

            if (value instanceof Value.Func func) {
                return func.application().apply(this, args);
            }

            return value;
        }

        return null;
    }

    public void resolve(Expression expr, int depth) {
        locals.put(expr, depth);
    }

    private Value lookUpVariable(String name, Expression expression) {
        Integer distance = locals.get(expression);
        if (distance != null) {
            return environment.at(distance, name);
        } else {
            return globals.value(name);
        }
    }

    public Value evaluate(Expression expression, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            return evaluate(expression);
        } finally {
            this.environment = previous;
        }
    }

}
