package ravi.model;

import ravi.resolver.Environment;
import ravi.resolver.Interpreter;
import ravi.syntax.ast.Expression;

import java.util.List;

public record Func(List<String> params, Expression expression, Environment closure) implements Application {

    @Override
    public Value apply(Interpreter inter, List<Value> args) {
        var environment = new Environment(closure);
        for (int i = 0; i < params.size() && i < args.size(); i++) {
            environment.define(params.get(i), args.get(i));
        }
        return inter.evaluate(expression, environment);
    }

    @Override
    public int arity() {
        return params.size();
    }

    public Environment closure() {
        return closure;
    }


}
