package ravi.infer;

import ravi.analysis.ast.*;
import ravi.core.Core;
import ravi.analysis.Token;

import java.util.*;
import java.util.stream.Collectors;

public final class Inference {

    private Integer freshVarCounter = 0;

    public record Couple(Substitution s, Type t) { }

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

            var e1 = application.expr();
            var e2 = application.args();

            var st1 = infer(context, e1);
            var s1 = st1.s;
            var t1 = st1.t;

            var sts2 = e2.stream()
                    .map(e -> infer(context.apply(s1), e))
                    .toList();
            var ts2 = sts2.stream()
                    .map(Couple::t)
                    .toList();

            var s2 = sts2.stream()
                    .map(Couple::s)
                    .reduce(Substitution::compose)
                    .orElse(Substitution.empty());

            var tau = fresh("a");

            var s3 = mgu(
                    t1.apply(s2),
                    new Type.TFunc(ts2, tau)
                );

            return new Couple(
                    s3.compose(s2).compose(s1),
                    tau.apply(s3)
                );
        }

        if (expression instanceof Expression.Lambda lambda) {

            var params = lambda
                    .parameters()
                    .declarations()
                    .stream()
                    .map(n -> Map.entry(
                            Nameable.stringOf(n),
                            fresh("a"))
                    ).toList();

            var schemes = params
                    .stream()
                    .map(e -> Map.entry(e.getKey(), makeScheme(e.getValue()))
                    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            var contextP = context.union(schemes);

            var st = infer(contextP, lambda.expr());
            var s1 = st.s;
            var t1 = st.t;

            var tauN = params
                    .stream()
                    .map(type -> type.getValue().apply(s1))
                    .toList();

            return new Couple(s1, new Type.TFunc(tauN, t1));
        }

        if (expression instanceof Expression.LetIn expr) {

            var in = compress(expr);
            var name = Nameable.stringOf(in.name());

            var ts = infer(context, in.expr());
            var s1 = ts.s;
            var t1 = ts.t;

            var scheme = generalize(context.apply(s1), t1);
            var contextP = context.union(Map.of(name, scheme));

            var ts2 = infer(contextP, in.result());
            var t2 = ts2.t;
            var s2 = ts2.s;

            return new Couple(s2.compose(s1), t2);
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

        if (expression instanceof Expression.Instr) {
            throw new RuntimeException("Not implemented yet.");
        }

        if (expression instanceof Expression.ListExpr listExpr) {
            if (listExpr.list() instanceof RaviList.List list) {
                var st = infer(context, list.head());
                var s1 = st.s;
                var t1 = st.t;
                var s2 = inferRaviList(s1, context, t1, list.tail());
                var t = t1.apply(s2);
                return new Couple(s2, new Type.TList(t));
            }
            return infer(new Constant.CEmptyList());
        }

        if (expression instanceof Expression.ApplicationOperator app) {
            return inferBinaryOperation(context, app.op(), app.left(), app.right());
        }

        if (expression instanceof Expression.Binary app) {
            return inferBinaryOperation(context, app.operator().symbolInfixOp(), app.left(), app.right());
        }

        if (expression instanceof Expression.PatternMatching expr) {

            var es = expr.expressions();
            var ps = expr.patterns();

            var st0 = infer(context, expr.expression());

            var stN = es.stream().map(e -> infer(context, e)).toList();
            var s = stN.stream()
                    .map(Couple::s)
                    .reduce(Substitution::compose)
                    .orElse(Substitution.empty());

            var t = stN.stream().findFirst().orElseThrow().t();

            return new Couple(s, t);
        }

        if (expression instanceof Expression.ConsCell consCell) {
            return inferBinaryOperation(context, Token.Symbol.DoubleColon, consCell.head(), consCell.tail());
        }

        if (expression instanceof Expression.ModuleCallExpr expr) {

        }

        if (expression instanceof Expression.Unary unary) {

        }

        throw new RuntimeException("Missing Implementation");
    }

    private Couple inferBinaryOperation(Context context, String name, Expression left, Expression right) {
        return infer(context, new Expression.Application(
            new Expression.IdentExpr(new Nameable.ValueName.NInfixOp(new Operator(name))),
                List.of(left, right)
        ));
    }

    private Substitution inferRaviList(Substitution s, Context context, Type pred, RaviRestList rest) {
        if (rest == null) return s;
        var st = infer(context, rest.expression());
        var sub = s.compose(mgu(pred, st.t()));
        return inferRaviList(sub, context, st.t().apply(s), rest.rest());
    }

    private Scheme makeScheme(Type tau) {
        return new Scheme(List.of(), tau);
    }

    public Type fresh(String prefix) {
        return new Type.TVar(prefix + freshVarCounter++);
    }

    public Type instantiate(Scheme scheme) {
        var nVars = scheme
                .forall()
                .stream()
                .map(s -> fresh("a"))
                .collect(Collectors.toList());
        var s = new Substitution(Core.zipToMap(scheme.forall(), nVars));
        return scheme.type().apply(s);
    }

    public Scheme generalize(Context context, Type type) {
        var typeFtv = new LinkedList<>(type.ftv());
        var ftv = context.ftv();
        typeFtv.removeAll(ftv);
        return new Scheme(typeFtv, type);
    }

    public Substitution varBind(String name, Type type) {
        if (type.ftv().contains(name))
            throw new RuntimeException("occurs check fails: " + name + " vs. " + type);

        return new Substitution(Map.of(name, type));
    }

    public Substitution mgu(Type t1, Type t2) {

        if (t1 instanceof Type.TFunc f1 && t2 instanceof Type.TFunc f2) {
            var s1 = mgu(f1.expr(), f2.expr());
            var s2 = s1;
            for (int i = 0; i < f1.params().size(); i++) {
                var p1 = f1.params().get(i);
                var p2 = f2.params().get(i);
                s2 = mgu(p1, p2).compose(s2);
            }
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

        if (t1 instanceof Type.TList l1 && t2 instanceof Type.TList l2) {
            return mgu(l1.type(), l2.type());
        }

        if (t1 instanceof Type.TList l1) {
            return mgu(l1.type(), t2);
        }

        if (t2 instanceof Type.TList l2) {
            return mgu(t1, l2.type());
        }

        if (t1 instanceof Type.TString && t2 instanceof Type.TString) {
            return Substitution.empty();
        }

        throw new RuntimeException("types do not unify: " + t1.toStr() + " with a " + t2.toStr());
    }

    public Couple infer(Context context, Pattern pattern) {

        if (pattern instanceof Pattern.PTuple tuple) {
            var cs = tuple.patterns().stream()
                    .map(p -> infer(context, p))
                    .toList();
            return new Couple(cs.)
        }

        if (pattern instanceof Pattern.PAny) {
            return new Couple(Substitution.empty(), fresh("a"));
        }

        if (pattern instanceof Pattern.PConstant constant) {
            return infer(constant.constant());
        }

        if (pattern instanceof Pattern.PLabelName) {
            return new Couple(Substitution.empty(), fresh("a"));
        }

        if (pattern instanceof Pattern.PCons cons) {
            var st0 = infer(context, cons.head());
            var st1 = infer(context, cons.tail());
            var s2 = mgu(st0.t(), st1.t());
            return new Couple(s2, fresh("a"));
        }

        throw new RuntimeException("Missing Implementation");
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
            return new Couple(Substitution.empty(), new Type.TList(fresh("a")));
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
        var ctx = infer(context, content.statement());
        return infer(ctx, content.restContent());
    }

    public Expression.LetIn compress(Expression.LetIn in) {
        if (in.parameters().declarations().isEmpty()) return in;
        var lambda = new Expression.Lambda(in.parameters(), in.expr());
        return new Expression.LetIn(in.name(), new Parameters(List.of()), lambda, in.result());
    }

    public Statement.Let compress(Statement.Let let) {
        if (let.parameters().declarations().isEmpty()) return let;
        var lambda = new Expression.Lambda(let.parameters(), let.expr());
        return new Statement.Let(let.name(), new Parameters(List.of()), lambda);
    }

}
