package ravi.infer;

import ravi.analysis.ast.*;
import ravi.core.Core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Inference {

    Map.Entry<Substitution, Type> infer(Context context, Expression expression) {

        if (expression instanceof Expression.ValueNameExpr expr) {
            String varName = Nameable.stringOf(expr.valueName());
            if (context.env().containsKey(varName)) {
                Scheme content = context.env().get(varName);
                Type type = instantiate(content);
                return Map.entry(Substitution.empty(), type);
            }
            throw new InferException("unbound variable: %s".formatted(varName));
        }

        if (expression instanceof Expression.LetIn expr) {
            var name = Nameable.stringOf(expr.valueName());
            var ts = infer(context, expr.expr());
            var ctx2 = inferParameters(context, expr.parameters());
            var ctx3 = inferLet(ctx2, name, ts);
            var ts2 = infer(ctx3.apply(ts.getKey()), expr.result());
            return Map.entry(ts.getKey().compose(ts2.getKey()), ts.getValue());
        }

        if (expression instanceof Expression.Application application) {

            var infer1 =
                    application
                    .args().stream()
                    .map(e -> infer(context, e))
                    .toList();

            var s1 =
                    infer1.stream()
                    .map(Map.Entry::getKey)
                    .reduce(Substitution::compose)
                    .orElse(Substitution.empty());

            var t1 =
                    infer1.stream()
                    .map(Map.Entry::getValue)
                    .toList();

            var infer2 = infer(context.apply(s1), application.expr());

            var type = new Type.TFunc(infer2.getValue(), t1);

            var s3 = mgu(infer2.getValue(), type);

            var sub = s3.compose(infer2.getKey()).compose(s1);

            return Map.entry(sub, type);
        }

        if (expression instanceof Expression.Lambda lambda) {

            List<Nameable.LabelName> decl = lambda
                    .parameters()
                    .declarations();

            List<Type> typesVar = decl.stream()
                    .map(Nameable::stringOf)
                    .map(this::newTyVar)
                    .toList();

            Context gammaPP = inferParameters(context, lambda.parameters());

            Map.Entry<Substitution, Type> inferE = infer(gammaPP, lambda.expression());

            var types = Typing.ListTyping
                    .of(typesVar.stream())
                    .apply(inferE.getKey())
                    .toList();

            return Map.entry(inferE.getKey(), new Type.TFunc(inferE.getValue(), types));
        }

        if (expression instanceof Expression.ConstantExpr expr) {
            return constant(expr.constant());
        }

        if (expression instanceof Expression.ParenthesisExpr expr) {
            return infer(context, expr.expr());
        }

        if (expression instanceof Expression.GroupExpr expr) {
            return infer(context, expr.expr());
        }

        throw new RuntimeException("Missing Implementation");
    }

    private Context inferLet(Context context, String name, Map.Entry<Substitution, Type> ts) {
        var ctx1 = context.remove(name);
        var tP = generalize(context.apply(ts.getKey()), ts.getValue());
        var env = new HashMap<>(ctx1.env());
        env.put(name, tP);
        return new Context(env);
    }

    private Context inferParameters(Context context, Parameters parameters) {

        List<String> names = parameters
                .declarations()
                .stream()
                .map(Nameable::stringOf)
                .toList();

        Context gammaP = context.removeAll(names);

        List<Type> typesVar = parameters
                .declarations()
                .stream()
                .map(Nameable::stringOf)
                .map(this::newTyVar)
                .toList();

        Map<String, Scheme> envPP =
                IntStream.range(0, parameters.declarations().size())
                        .boxed()
                        .collect(Collectors.toMap(names::get,
                                i -> new Scheme(List.of(), typesVar.get(i))));

        var e = gammaP.union(envPP);
        return  e;
    }

    Type newTyVar(String prefix) {
        return new Type.TVar(prefix);
    }

    Type instantiate(Scheme scheme) {

        var nVars = scheme
                .forall()
                .stream()
                .map(this::newTyVar)
                .collect(Collectors.toList());

        return scheme.type().apply(new Substitution(Core.zipToMap(scheme.forall(), nVars)));
    }

    Scheme generalize(Context context, Type type) {
        var set = new HashSet<>(type.ftv());
        set.removeAll(context.ftv());
        return new Scheme(set.stream().toList(), type);
    }

    Substitution varBind(String name, Type type) {

        if (type instanceof Type.TVar)
            return Substitution.empty();

        if (type.ftv().contains(name))
            throw new RuntimeException("occurs check fails: " + name + " vs. " + type);

        return new Substitution(Map.of(name, type));
    }

    Substitution mgu(Type t1, Type t2) {

        if (t1 instanceof Type.TFunc f1 && t2 instanceof Type.TFunc f2) {
            var s1 = mgu(f1.expr(), f2.expr());
            var s2 = Substitution.empty();

            for (var p : f2.params())
                s2 = s2.compose(mgu(f1.expr().apply(s1), p.apply(s1)));

            return s1.compose(s2);
        }

        if (t1 instanceof Type.TVar var) {
            return varBind(var.name(), t2);
        }

        if (t2 instanceof Type.TVar var) {
            return varBind(var.name(), t1);
        }

        if (t1 instanceof Type.TInt && t2 instanceof Type.TInt) {
            return Substitution.empty();
        }

        if (t1 instanceof Type.TFloat && t2 instanceof Type.TFloat) {
            return Substitution.empty();
        }

        throw new RuntimeException("types do not unify: " + t1 + " vs. " + t2);
    }

    Map.Entry<Substitution, Type> constant(Constant constant) {
        if (constant instanceof Constant.CFloat) {
            return Map.entry(Substitution.empty(), new Type.TFloat());
        }
        if (constant instanceof Constant.CInt) {
            return Map.entry(Substitution.empty(), new Type.TInt());
        }
        if (constant instanceof Constant.CString) {
            return Map.entry(Substitution.empty(), new Type.TString());
        }
        if (constant instanceof Constant.CText) {
            return Map.entry(Substitution.empty(), new Type.TString());
        }
        if (constant instanceof Constant.CUnit) {
            return Map.entry(Substitution.empty(), new Type.TUnit());
        }
        if (constant instanceof Constant.CEmptyList) {
            return Map.entry(Substitution.empty(), new Type.TList());
        }
        throw new RuntimeException("Missing Implementation");
    }

    public Context infer(Context context, Program program) {
        if (program == null) return context;
        var c = infer(context, program.statement());
        c = infer(c, program.program());
        return c;
    }

    private Context infer(Context context, Statement statement) {

        if (statement instanceof Statement.Instr instr) {
            Context c = context;
            for (var expression : instr.expression()) {
                var ts = infer(c, expression);
                c = c.apply(ts.getKey());
            }
            return c;
        }

        if (statement instanceof Statement.Module module) {
            return infer(context, module.moduleContent());
        }

        if (statement instanceof Statement.Let let) {
            var c = inferLet(context, Nameable.stringOf(let.name()), let.parameters(), let.result());
            System.out.println(c);
            return c;
        }

        throw new RuntimeException("Missing Implementation");
    }

    private Context inferLet(Context context, String name, Parameters parameters, Expression expression) {
        var ts = infer(context, expression);
        var a = inferLet(context, name, ts);
        var c = inferParameters(a, parameters);
        return c;
    }

    private Context infer(Context context, ModuleContent content) {
        if (content == null) return new Context();
        infer(context, content.let());
        return infer(context, content.restContent());
    }

}
