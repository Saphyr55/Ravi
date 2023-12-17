package ravi.resolver;

import ravi.model.Func;
import ravi.model.Value;
import ravi.syntax.ast.*;

import java.util.ArrayList;
import java.util.List;

public class Interpreter {

    private Environment environment;

    public Interpreter(Environment context) {
        this.environment = context;
    }

    public void interpretProgram(Program program) {
        if (program != null) {
            interpretInstruction(program.statement());
            interpretProgram(program.program());
        }
    }

    void interpretInstruction(Statement statement) {
        if (statement instanceof Statement.Let let) {
            defineFunction(environment, let.name(), let.params(), let.result());
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

        if (expression instanceof Expression.PatternMatching pm) {
            Value value = evaluate(pm.expression());
            for (int i = 0; i < pm.patterns().size(); i++) {
                if (patternMatch(pm.patterns().get(i), value)) {
                    return evaluate(pm.expressions().get(i));
                }
            }
            return Value.unit();
        }

        if (expression instanceof Expression.UnitExpr) {
            return Value.unit();
        }

        if (expression instanceof  Expression.ListExpr expr) {
            ArrayList<Value> values = new ArrayList<>();
            if (expr.list() instanceof RaviList.EmptyList){
                return new Value.VList(new ArrayList<>());
            } else if (expr.list() instanceof RaviList.List list) {
                values.add(evaluate(list.head()));
                evaluateList(list.tail(),values);
            }
            return Value.list(values);
        }

        if (expression instanceof Expression.ConstantExpr expr) {
            return evaluate(expr.constant());
        }

        if (expression instanceof Expression.ParenthesisExpr expr) {
            return evaluate(expr.expr());
        }

        if (expression instanceof Expression.GroupExpr expr) {
            return evaluate(expr.expr());
        }

        if (expression instanceof Expression.IdentifierExpr expr) {
            return lookUpDeclaration(expr.identifier().name());
        }

        if (expression instanceof Expression.Instr) {
            throw new InterpretException("Not implemented yet.");
        }

        if (expression instanceof Expression.LetIn expr)  {
            defineFunction(environment, expr.name(), expr.params(), expr.expr());
            return evaluate(expr.result(), environment);
        }

        if (expression instanceof Expression.Application application) {

            Value value = evaluate(application.expr());

            List<Value> args = application
                    .args()
                    .stream()
                    .map(this::evaluate)
                    .map(v -> {
                        if (v instanceof Value.VApplication f && f.application().arity() == 0) {
                            return f.application().apply(this, List.of());
                        }
                        return v;
                    })
                    .toList();

            if (value instanceof Value.VApplication VApplication) {
                return VApplication.application().apply(this, args);
            }

            return value;
        }

        return null;
    }

    private boolean patternMatch(Pattern pattern, Value value) {

        if (pattern instanceof Pattern.PAny) {
            return true;
        }

        if (pattern instanceof Pattern.PIdentifier identifier) {
            environment.define(identifier.identifier().name(), value);
            return true;
        }

        if (pattern instanceof Pattern.PCons cons && value instanceof Value.VList list) {
            return !list.values().isEmpty() &&
                    patternMatch(cons.head(), list.values().get(0)) &&
                    patternMatch(cons.tail(), new Value.VList(list.values().subList(1, list.values().size())));
        }

        if (pattern instanceof Pattern.PConstant constant) {
            Value v = evaluate(constant.constant());
            return value.equals(v);
        }

        if (pattern instanceof Pattern.PGroup pGroup) {
            return patternMatch(pGroup.inner(), value);
        }

        return false;
    }

    private Value evaluate(Constant constant) {

        if (constant instanceof Constant.CString cString) {
            return new Value.VString(cString.content());
        }

        if (constant instanceof Constant.CText cText) {
            return new Value.VString(cText.content());
        }

        if (constant instanceof Constant.CEmptyList) {
            return new Value.VList(List.of());
        }

        if (constant instanceof Constant.CNumber number) {
            return new Value.VNumber(number.number());
        }

        if (constant instanceof Constant.CUnit) {
            return new Value.VUnit();
        }

        throw new InterpretException();
    }

    private Value lookUpDeclaration(String name) {
        return environment.search(name);
    }

    public Value evaluate(Expression expression, Environment environment) {
        Environment previous = this.environment;
        Value value;
        try {
            this.environment = environment;
            value = evaluate(expression);
        } finally {
            this.environment = previous;
        }
        return value;
    }

    void defineFunction(Environment env, Identifier name, Params params, Expression result) {

        if (params.declarations().isEmpty()) {
            env.define(name.name(), evaluate(result, env));
            return;
        }

        env.define(name.name(),
                new Value.VApplication(new Func(params
                    .declarations()
                    .stream()
                    .map(Identifier::name)
                    .toList(), result, env)));
    }


}
