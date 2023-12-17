package ravi.resolver;

import ravi.model.Func;
import ravi.model.Value;
import ravi.syntax.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
            defineFunction(environment, let.name(), let.parameters(), let.result());
        }

        else if (statement instanceof Statement.Module module) {
            String moduleName = module.moduleName().name();
            Environment moduleEnv = new Environment(environment);
            environment.define(moduleName, Value.module(moduleName, moduleEnv));
            interpretModuleContent(moduleEnv, module.moduleContent());
        }

        else if (statement instanceof Statement.Instr instr) {
            instr.expression().forEach(this::evaluate);
        }

        
    }

    void interpretModuleContent(Environment environment, ModuleContent content) {
        if (content == null) return;
        Statement.Let let = content.let();
        defineFunction(environment, let.name(), let.parameters(), let.result());
        interpretModuleContent(environment, content.restContent());
    }

    void evaluateList(RaviRestList rest, List<Value> values) {
        if(rest == null)
            return;
        values.add(evaluate(rest.expression()));
        evaluateList(rest.rest(),values);
    }

    Value evaluate(Expression expression) {

        if (expression instanceof Expression.ModuleCallExpr expr) {
            Value value = environment.value(expr.moduleName().name());
            if (value instanceof Value.VModule module) {
                return module.environment().get(expr.valueName().name());
            }
            throw new InterpretException("");
        }

        if (expression instanceof Expression.ConsCell consCell) {
            Value head = evaluate(consCell.head());
            Value tail = evaluate(consCell.tail());
            if (tail instanceof Value.VList list) {
                return Value.list(Stream.concat(Stream.of(head), list.values().stream()).toList());
            }
            throw new InterpretException();
        }

        if (expression instanceof Expression.PatternMatching pm) {
            Value value = evaluate(pm.expression());
            for (int i = 0; i < pm.patterns().size(); i++) {
                if (patternMatch(pm.patterns().get(i), value)) {
                    return evaluate(pm.expressions().get(i));
                }
            }
            throw new InterpretException("Missing '_' pattern for '%s' moduleName."
                            .formatted(value.toStr()));
        }

        if (expression instanceof Expression.Lambda lambda) {
            return new Value.VApplication(new Func(lambda.parameters()
                    .declarations()
                    .stream()
                    .map(Identifier.Lowercase::name)
                    .toList(), lambda.expression(), environment));
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

        if (expression instanceof Expression.ValueNameExpr expr) {
            return lookUpDeclaration(expr.valueName().name());
        }

        if (expression instanceof Expression.Instr) {
            throw new InterpretException("Not implemented yet.");
        }

        if (expression instanceof Expression.LetIn expr)  {
            defineFunction(environment, expr.valueName(), expr.parameters(), expr.expr());
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

        if (pattern instanceof Pattern.PAny) return true;

        if (pattern instanceof Pattern.PValueName identifier) {
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

        throw new IllegalStateException("Missing pattern implementation.");
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

        if (constant instanceof Constant.CInt integer) {
            return new Value.VInt(integer.integer());
        }

        if (constant instanceof Constant.CFloat cFloat){
            return new Value.VFloat(cFloat.cFloat());
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

    void defineFunction(Environment env, Identifier.Lowercase name, Parameters parameters, Expression result) {

        if (parameters.declarations().isEmpty()) {
            env.define(name.name(), evaluate(result, env));
            return;
        }

        env.define(name.name(),
                new Value.VApplication(new Func(parameters
                    .declarations()
                    .stream()
                    .map(Identifier.Lowercase::name)
                    .toList(), result, env)));
    }


}
