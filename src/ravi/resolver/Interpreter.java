package ravi.resolver;

import ravi.model.Func;
import ravi.model.Value;
import ravi.syntax.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter {

    private final Map<Expression, Integer> locals;
    private final Environment globals;
    private Environment environment;

    public Interpreter(Environment context) {
        this.locals = new HashMap<>();
        this.environment = context;
        this.globals = environment;
    }

    public void interpretProgram(Program program) {
        if (program != null) {
            interpretInstruction(program.statement());
            interpretProgram(program.program());
        }
    }

    void interpretInstruction(Statement statement) {
        if (statement instanceof Statement.Let let) {
            var name = let.name().identifier();
            var params = let.params()
                    .declarations()
                    .stream()
                    .map(Identifier::identifier)
                    .toList();

            if (params.size() == 0) {
                environment.define(name, evaluate(let.result()));
                return;
            }

            environment.define(name, new Value.Func(new Func(params, let.result(), environment)));
            return;
        }
        if (statement instanceof Statement.Instr instr) {
            instr.expression().forEach(this::evaluate);
            return;
        }
    }

    void evaluateList(RaviRestList rest,List<Value> values) {
        if(rest == null)
            return;
        values.add(evaluate(rest.expression()));
        evaluateList(rest.rest(),values);
    }

    Value evaluate(Expression expression) {

        if (expression instanceof  Expression.ListExpr expr) {
            ArrayList<Value> values = new ArrayList<>();
            if (expr.list() instanceof RaviList.EmptyList){
                return new Value.VList(new ArrayList<>());
            } else if (expr.list() instanceof RaviList.List list) {
                evaluateList(list.rest(),values);
            }
            return new Value.VList(values);
        }

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

        if (expression instanceof Expression.Instr expr) {
            evaluate(expr.primary());
            var e = evaluate(expr.result());
            return e;
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
