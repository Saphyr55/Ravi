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
                var type = instantiate(scheme);
                return new Couple(Substitution.empty(), type);
            }
            throw new InferException("unbound variable: %s".formatted(varName));
        }

        if (expression instanceof Expression.Application application) {

            var app = compress(application);
            var e1 = app.expr();
            var e2 = app.args().get(0);

            var tauP = fresh("a");

            var st1 = infer(context, e1);
            var s1 = st1.s;
            var t1 = st1.t;

            var st2 = infer(context.apply(s1), e2);
            var s2 = st2.s;
            var t2 = st2.t;

            var s3 = mgu(
                    t1.apply(s2),
                    new Type.TFunc(List.of(t2), tauP)
            );

            return new Couple(
                    s3.compose(s2).compose(s1),
                    tauP.apply(s3)
            );
        }

        if (expression instanceof Expression.Lambda lambda) {

            var lambdaP = compress(lambda);
            var param = lambdaP.parameters().declarations().get(0);
            var varParam = Nameable.stringOf(param);

            var tau = fresh("a");

            var contextP = context.remove(varParam);
            var contextPP = contextP.union(Map.of(varParam, dontGeneralize(tau)));
            var st = infer(contextPP, lambdaP.expr());
            var s1 = st.s;
            var t1 = st.t;

            return new Couple(s1, new Type.TFunc(List.of(tau.apply(s1)), t1));
        }

        if (expression instanceof Expression.LetIn expr) {

            var in = compress(expr);
            var name = Nameable.stringOf(in.valueName());

            var ts = infer(context, in.expr());
            var s1 = ts.s;
            var t1 = ts.t;

            var scheme = generalize(context.apply(s1), t1);
            var contextP = context.union(Map.of(name, scheme));

            System.out.println(contextP);
            System.out.println("-------");

            var ts2 = infer(contextP, in.result());
            var t2 = ts2.t;
            var s2 = ts2.s;

            return new Couple(s1.compose(s2), t2);
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

    private Scheme dontGeneralize(Type tau) {
        return new Scheme(List.of(), tau);
    }

    public Type fresh(String prefix) {
        return new Type.TVar(prefix + freshVarCounter++);
    }

    public Type instantiate(Scheme scheme) {
        var nVars = scheme
                .forall()
                .stream()
                .map(this::fresh)
                .collect(Collectors.toList());
        var s = new Substitution(Core.zipToMap(scheme.forall(), nVars));
        return scheme.type().apply(s);
    }

    public Scheme generalize(Context context, Type type) {
        var set = new LinkedList<>(type.ftv());
        var ftv = context.ftv();
        set.removeAll(ftv);
        return new Scheme(set, type);
    }

    public Substitution varBind(String name, Type type) {

        if (type instanceof Type.TVar)
            return Substitution.empty();

        if (type.ftv().contains(name))
            throw new RuntimeException("occurs check fails: " + name + " vs. " + type);

        return new Substitution(Map.of(name, type));
    }

    public Substitution mgu(Type t1, Type t2) {

        if (t1 instanceof Type.TFunc f1 && t2 instanceof Type.TFunc f2) {
            var s1 = mgu(f1.expr(), f2.expr());
            var s2 = mgu(f1.params().get(0).apply(s1), f2.params().get(0).apply(s1));
            return s2.compose(s1);
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

        if (statement instanceof Statement.Instr) {
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
            var lIn = new Expression.Lambda(new Parameters(Collections.singletonList(param)), lambda.expr());
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
