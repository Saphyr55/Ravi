package ravi.infer;

import ravi.analysis.ast.*;
import ravi.core.Core;

import java.util.*;
import java.util.stream.Collectors;

public final class Inference {

    private Integer freshVarCounter = 0;

    record Couple(Substitution s, Type t) { }

    public Couple infer(Context context, Expression expression) {

        if (expression instanceof Expression.IdentExpr expr) {
            String varName = Nameable.stringOf(expr.valueName());
            if (context.env().containsKey(varName)) {
                Scheme scheme = context.env().get(varName);
                return new Couple(Substitution.empty(), instantiate(scheme));
            }
            throw new InferException("unbound variable: %s".formatted(varName));
        }

        if (expression instanceof Expression.Application application) {

            var app = compress(application);
            var e1 = app.expr();
            var e2 = app.args().get(0);

            var st0 = infer(context, e1);
            var s0 = st0.s;
            var t0 = st0.t;

            var st1 = infer(context.apply(s0), e2);
            var s1 = st1.s;
            var t1 = st1.t;

            var tauP = fresh();
            var s2 = unify(t0.apply(s1), new Type.TFunc(List.of(t1), tauP));

            return new Couple(s2.compose(s1).compose(s0), tauP.apply(s2));
        }

        if (expression instanceof Expression.Lambda lambda) {

            var lambdaP = compress(lambda);
            var param = lambdaP.parameters().declarations().get(0);
            var varParam = Nameable.stringOf(param);

            var tau = fresh();
            var scheme = dontGeneralize(context, tau);
            var contextP = context.union(Map.of(varParam, scheme));
            var st = infer(contextP, lambdaP.expression());

            return new Couple(st.s, new Type.TFunc(List.of(tau.apply(st.s)), st.t));
        }

        if (expression instanceof Expression.LetIn expr) {
            // Example : let f x y = e  become  let f = fun x -> fun y -> e
            var in = compress(expr);
            var name = Nameable.stringOf(in.valueName());

            var ts = infer(context, in.expr());
            var s1 = ts.s;
            var varType = ts.t;

            var scheme = generalize(context, varType);
            var contextP = context.union(Map.of(name, scheme));
            var ts2 = infer(contextP, in.expr());

            return new Couple(s1.compose(ts2.s), ts2.t);
        }

        if (expression instanceof Expression.ConstantExpr expr) {
            return infer(expr.constant());
        }

        if (expression instanceof Expression.ParenthesisExpr expr) {
            return infer(context, expr.expr());
        }

        if (expression instanceof Expression.GroupExpr expr) {
            return infer(context, expr.expr());
        }

        if (expression instanceof Expression.UnitExpr) {
            return new Couple(Substitution.empty(), new Type.TUnit());
        }

        throw new RuntimeException("Missing Implementation");
    }

    private Scheme dontGeneralize(Context context, Type tau) {
        return new Scheme(List.of(), tau);
    }

    public Type fresh() {
        return new Type.TVar("α" + freshVarCounter++);
    }

    public Type instantiate(Scheme scheme) {
        var nVars = scheme.forall().stream()
                .map(s -> fresh())
                .collect(Collectors.toList());

        var newSub = new Substitution(Core.zipToMap(scheme.forall(), nVars));
        return scheme.type().apply(newSub);
    }

    public Scheme generalize(Context context, Type type) {
        var set = new HashSet<>(type.ftv());
        set.removeAll(context.ftv());
        return new Scheme(set.stream().toList(), type);
    }

    public Substitution varBind(String name, Type type) {

        if (type instanceof Type.TVar)
            return Substitution.empty();

        if (type.ftv().contains(name))
            throw new RuntimeException("occurs check fails: " + name + " vs. " + type);

        return new Substitution(Map.of(name, type));
    }

    public Substitution unify(Type t1, Type t2) {

        if (t1 instanceof Type.TFunc f1 && t2 instanceof Type.TFunc f2) {
            var s1 = unify(f1.expr(), f2.expr());
            var s2 = unify(f1.params().get(0), f2.params().get(0));
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

        if (t1 instanceof Type.TUnit && t2 instanceof Type.TUnit) {
            return Substitution.empty();
        }

        throw new RuntimeException("types do not unify: " + t1 + " vs. " + t2);
    }

    public Couple infer(Constant constant) {
        if (constant instanceof Constant.CFloat) {
            return new Couple(Substitution.empty(), new Type.TFloat());
        }
        if (constant instanceof Constant.CInt) {
            return new Couple(Substitution.empty(), new Type.TInt());
        }
        if (constant instanceof Constant.CString) {
            return new Couple(Substitution.empty(), new Type.TString());
        }
        if (constant instanceof Constant.CText) {
            return new Couple(Substitution.empty(), new Type.TString());
        }
        if (constant instanceof Constant.CUnit) {
            return new Couple(Substitution.empty(), new Type.TUnit());
        }
        if (constant instanceof Constant.CEmptyList) {
            return new Couple(Substitution.empty(), new Type.TList());
        }
        throw new RuntimeException("Missing Implementation");
    }

    public Context infer(Context context, Program program) {
        if (program == null) return context;
        var c = infer(context, program.statement());
        c = infer(c, program.program());
        return c;
    }

    public Context infer(Context context, Statement statement) {

        if (statement instanceof Statement.Instr instr) {
            for (var expression : instr.expression()) {
                infer(context, expression);
            }
            return context;
        }

        if (statement instanceof Statement.Module module) {
            return infer(context, module.moduleContent());
        }

        if (statement instanceof Statement.Let let) {
            // Example : let f x y = e  become  let f = fun x -> fun y -> e

            var letP = compress(let);
            var name = Nameable.stringOf(letP.name());

            var ts = infer(context, letP.expr());
            var tau = ts.t;
            var s0 = ts.s;

            var scheme = generalize(context, tau.apply(s0));

            return context.union(Map.of(name, scheme));
        }

        throw new RuntimeException("Missing Implementation");
    }

    public Context infer(Context context, ModuleContent content) {
        if (content == null) return new Context();
        var ctx = infer(context, content.let());
        return infer(ctx, content.restContent());
    }

    public Expression.Application compress(Expression.Application application) {
        var dcl = application.args();
        if (application.args().size() > 1) {
            var sub = dcl.subList(1, dcl.size());
            var arg = dcl.get(0);
            var lIn = new Expression.Application(application.expr(), Collections.singletonList(arg));
            return compress(new Expression.Application(lIn, sub));
        }
        return application;
    }

    public Expression.Lambda compress(Expression.Lambda lambda) {
        var dcl = lambda.parameters().declarations();
        if (dcl.size() > 1) {
            var sub = dcl.subList(0, dcl.size() - 1);
            var param = dcl.get(dcl.size() - 1);
            var lIn = new Expression.Lambda(new Parameters(Collections.singletonList(param)), lambda.expression());
            return compress(new Expression.Lambda(new Parameters(sub), lIn));
        }
        return lambda;
    }

    public Expression.LetIn compress(Expression.LetIn in) {
        var dcl = in.parameters().declarations();
        if (dcl.size() >= 1) {
            var lambda = new Expression.Lambda(new Parameters(dcl), in.expr());
            var nIn = new Expression.LetIn(in.valueName(), new Parameters(List.of()), compress(lambda), in.result());
            return compress(nIn);
        }
        return in;
    }

    public Statement.Let compress(Statement.Let let) {
        var dcl = let.parameters().declarations();
        if (dcl.size() >= 1) {
            var lambda = new Expression.Lambda(new Parameters(dcl), let.expr());
            var letP = new Statement.Let(let.name(), new Parameters(List.of()), compress(lambda));
            return compress(letP);
        }
        return let;
    }



}
