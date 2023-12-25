package ravi.resolver;

import ravi.model.Application;
import ravi.model.Func;
import ravi.model.Value;
import ravi.analysis.ast.*;
import ravi.analysis.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class Interpreter {

    private Environment environment;

    public Interpreter(Environment context) {
        this.environment = context;
    }

    public void interpretProgram(Program program) {
        if (program != null) {
            interpretStmt(program.statement());
            interpretProgram(program.program());
        }
    }

    public void interpretStmt(Statement statement) {

        if (statement instanceof Statement.Let let) {
            String name = Nameable.stringOf(let.name());
            defineFunction(environment, name, let.parameters(), let.expr());
        }

        else if (statement instanceof Statement.Module module) {
            String moduleName = Nameable.stringOf(module.moduleName());
            Environment moduleEnv = new Environment(environment);
            environment.define(moduleName, Value.module(moduleName, moduleEnv));
            interpretModuleContent(moduleEnv, module.moduleContent());
        }

        else if (statement instanceof Statement.ADT adt) {
            defineADTType(environment, adt);
        }

        else if (statement instanceof Statement.Instr instr) {
            instr.expression().forEach(this::evaluate);
        }

    }

    void interpretModuleContent(Environment environment, ModuleContent content) {
        if (content == null) return;
        if (content.statement() instanceof Statement.Let let) {
            defineFunction(environment, Nameable.stringOf(let.name()), let.parameters(), let.expr());
        }
        else if (content.statement() instanceof Statement.ADT adt) {
            defineADTType(environment, adt);
        }
        interpretModuleContent(environment, content.restContent());
    }

    void defineADTType(Environment environment, Statement.ADT adt) {
        adt.typesConstructors().forEach((caseName, typeExpression) -> {
            String name = Nameable.stringOf(caseName);
            if (typeExpression == null) {
                environment.define(name, Value.adt(name, Value.unit()));
                return;
            }
            environment.define(name, Application.value(1, (inter, args) -> {
                var arg = args
                        .stream()
                        .findFirst()
                        .orElseThrow(RuntimeException::new);
                return Value.adt(name, arg);
            }));
        });
    }

    void evaluateList(RaviRestList rest, List<Value> values) {
        if(rest == null)
            return;
        values.add(evaluate(rest.expression()));
        evaluateList(rest.rest(),values);
    }

    Value evaluate(Expression expression) {

        if (expression instanceof Expression.ApplicationOperator appOp) {
            Value value = environment.search(appOp.op());
            if (value instanceof Value.VApplication application) {
                return application.application().apply(this, List.of(
                        evaluate(appOp.right()),
                        evaluate(appOp.left()))
                );
            }
            throw new InterpretException("( %s ) is not an application".formatted(appOp.op()));
        }

        if (expression instanceof Expression.ModuleCallExpr expr) {
            Value value = environment.value(expr.moduleName().name().id());
            if (value instanceof Value.VModule module) {
                return module.environment().get(Nameable.stringOf(expr.valueName()));
            }
            throw new InterpretException("");
        }

        if (expression instanceof Expression.Tuple tuple) {
            return new Value.VTuple(tuple
                    .expressions()
                    .stream()
                    .map(this::evaluate)
                    .toList());
        }

        if (expression instanceof Expression.PatternMatching pm) {
            Value value = evaluate(pm.expression());
            for (int i = 0; i < pm.patterns().size(); i++) {
                if (patternMatch(pm.patterns().get(i), value)) {
                    return evaluate(pm.expressions().get(i));
                }
            }
            throw new InterpretException("Missing '_' pattern for '%s' id."
                            .formatted(value.toStr()));
        }

        if (expression instanceof Expression.Lambda lambda) {
            return Value.application(new Func(lambda.parameters()
                    .declarations()
                    .stream()
                    .map(Nameable::stringOf)
                    .toList(), lambda.expr(), environment));
        }

        if (expression instanceof Expression.UnitExpr) {
            return Value.unit();
        }

        if (expression instanceof  Expression.ListExpr expr) {

            List<Value> values = new ArrayList<>();

            if (expr.list() instanceof RaviList.EmptyList){
                return new Value.VList(List.of());
            }

            if (expr.list() instanceof RaviList.List list) {
                values.add(evaluate(list.head()));
                evaluateList(list.tail(), values);
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

        if (expression instanceof Expression.IdentExpr expr) {
            return lookUpDeclaration(Nameable.stringOf(expr.valueName()));
        }

        if (expression instanceof Expression.Instr) {
            throw new InterpretException("Not implemented yet.");
        }

        if (expression instanceof Expression.LetIn expr)  {
            defineFunction(environment, Nameable.stringOf(expr.name()), expr.parameters(), expr.expr());
            return evaluate(expr.result(), environment);
        }

        if (expression instanceof Expression.Binary binary) {
            return binary(binary);
        }

        if (expression instanceof Expression.Application application) {

            var value = evaluate(application.expr());
            if (value instanceof Value.VApplication vApplication) {

                return applyValueApplication(vApplication, application.args()
                        .stream()
                        .map(this::evaluate)
                        .toList());

            }
            throw new InterpretException("You try to pass argument to a not function.");
        }

        return null;
    }

    private Value binary(Expression.Binary binary) {

        if (binary.operator().symbolInfixOp().equals(Token.Symbol.Slash)) {
            var left = (Value.VInt) evaluate(binary.left());
            var right = (Value.VInt) evaluate(binary.right());
            return Value.integer(left.integer() * right.integer());
        }

        if (binary.operator().symbolInfixOp().equals(Token.Symbol.Asterisk)) {
            var left = (Value.VInt) evaluate(binary.left());
            var right = (Value.VInt) evaluate(binary.right());
            return Value.integer(left.integer() * right.integer());
        }

        if (binary.operator().symbolInfixOp().equals(Token.Symbol.Minus)) {
            var left = (Value.VInt) evaluate(binary.left());
            var right = (Value.VInt) evaluate(binary.right());
            return Value.integer(left.integer() - right.integer());
        }

        if (binary.operator().symbolInfixOp().equals(Token.Symbol.Plus)) {
            var left = (Value.VInt) evaluate(binary.left());
            var right = (Value.VInt) evaluate(binary.right());
            return Value.integer(left.integer() + right.integer());
        }

        throw new InterpretException("");
    }

    private Value applyValueApplication(Value.VApplication application, List<Value> args) {

        var arity = application.application().arity();

        if (args.size() > arity) {
            var l = args.subList(0, arity);
            Value value = application.application().apply(this, l);
            if (value instanceof Value.VApplication second) {
                var l2 = args.subList(arity, args.size());
                return second.application().apply(this, l2);
            }
            throw new InterpretException("You try to pass to much argument in the function.");
        }

        if (args.size() < arity) {
            return Application.value(arity - args.size(), (inter, futureArgs) -> {
                var newArgs = Stream.concat(args.stream(), futureArgs.stream());
                return application.application().apply(inter, newArgs.toList());
            });
        }

        return application.application().apply(this, args);
    }

    private boolean patternMatch(Pattern pattern, Value value) {

        if (pattern instanceof Pattern.PAny) return true;

        if (pattern instanceof Pattern.PLabelName name) {
            environment.define(Nameable.stringOf(name.labelName()), value);
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

        if (pattern instanceof Pattern.PAdt pAdt && value instanceof Value.VAlgebraicDataType adt) {
            if (!adt.name().equals(Nameable.stringOf(pAdt.name()))) {
                return false;
            }
            if (pAdt.pattern() != null) {
                return patternMatch(pAdt.pattern(), adt.value());
            }
            return true;
        }

        if (pattern instanceof Pattern.PAdt) {
            return false;
        }

        if (pattern instanceof Pattern.PTuple pTuple && value instanceof Value.VTuple vTuple) {

            if (pTuple.patterns().size() != vTuple.values().size())
                throw new InterpretException("Can not match.");

            for (int i = 0; i < pTuple.patterns().size(); i++) {
                var t = pTuple.patterns().get(i);
                var v = vTuple.values().get(i);
                if (!patternMatch(t, v))
                    return false;
            }

            return true;
        }

        if (pattern instanceof Pattern.PTuple pTuple) {

            if (pTuple.patterns().size() == 1)
                return patternMatch(pTuple.patterns().get(0), value);

            throw new IllegalStateException("Missing '_' pattern.");
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

    void defineFunction(Environment env, String name, Parameters parameters, Expression result) {

        List<String> params = parameters
                .declarations()
                .stream()
                .map(Nameable::stringOf)
                .toList();

        if (parameters.declarations().isEmpty()) {
            env.define(name, evaluate(result, env));
            return;
        }

        env.define(name, new Value.VApplication(new Func(params, result, env)));
    }

}
