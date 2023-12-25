package ravi.infer;

import ravi.analysis.ast.*;
import ravi.core.Core;
import ravi.analysis.Token;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Inference {

    private static Integer freshVarCounter = 0;

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

            var contextP = context.union(Map.of(name, makeScheme(fresh("a"))));

            var ts = infer(contextP, in.expr());
            var s1 = ts.s;
            var t1 = ts.t;

            var scheme = generalize(contextP.apply(s1), t1);
            contextP = contextP.union(Map.of(name, scheme));

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

        if (expression instanceof Expression.Tuple tuple) {
            var cs = tuple.expressions().stream().map(e -> infer(context, e)).toList();
            var s = cs.stream().map(Couple::s).reduce(Substitution::compose).orElse(Substitution.empty());
            return new Couple(s, new Type.TTuple(cs.stream().map(Couple::t).toList()));
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

            var withE = infer(context, expr.expression());

            var tau = fresh("a");

            var s = IntStream.range(0, es.size())
                    .boxed()
                    .map(i -> {
                        var e = es.get(i);
                        var p = ps.get(i);

                        var c = infer(context, p);
                        var e1 = infer(c, e);

                        return mgu(withE.t, e1.t);
                    })
                    .reduce(Substitution::compose)
                    .orElse(Substitution.empty());

            var s2 = s.compose(withE.s);

            return new Couple(s2, tau.apply(s2));
        }

        if (expression instanceof Expression.ConsCell consCell) {
            return inferBinaryOperation(context, Token.Symbol.DoubleColon, consCell.head(), consCell.tail());
        }

        if (expression instanceof Expression.ModuleCallExpr expr) {
            return infer(context, new Expression.IdentExpr(expr.valueName()));
        }

        if (expression instanceof Expression.Unary unary) {
            return infer(context, unary.right());
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

        if (t1 instanceof Type.TTuple tuple1 && t2 instanceof Type.TTuple tuple2) {
            var s = Substitution.empty();
            for (int i = 0; i < tuple1.types().size(); i++) {
                var p1 = tuple1.types().get(i);
                var p2 = tuple2.types().get(i);
                s = mgu(p1, p2).compose(s);
            }
            return s;
        }

        if (t1 instanceof Type.TString && t2 instanceof Type.TString) {
            return Substitution.empty();
        }

        throw new RuntimeException("types do not unify: " + t1.toStr() + " with a " + t2.toStr());
    }

    public Context infer(Context context, Pattern pattern) {

        if (pattern instanceof Pattern.PAdt adt) {
            var n  = new Nameable.ValueName.NType(adt.name().name());
            var c  = infer(context, new Expression.IdentExpr(n));
            if (adt.pattern() == null) { return context.apply(c.s); }
            var c2 = infer(context, adt.pattern());
            return c2.apply(c.s);
        }

        if (pattern instanceof Pattern.PTuple tuple) {
            return tuple.patterns().stream()
                    .map(p -> {
                        Objects.requireNonNull(p);
                        return infer(context, p);
                    })
                    .reduce(Context::union)
                    .orElse(context);
        }

        if (pattern instanceof Pattern.PAny) {
            return context;
        }

        if (pattern instanceof Pattern.PConstant constant) {
            var c = infer(constant.constant());
            return context.apply(c.s);
        }

        if (pattern instanceof Pattern.PLabelName labelName) {
            var name = Nameable.stringOf(labelName.labelName());
            return context.union(Map.of(name, generalize(context, fresh("a"))));
        }

        if (pattern instanceof Pattern.PCons cons) {
            var st0 = infer(context, cons.head());
            var st1 = infer(context, cons.tail());
            return st0.union(st1);
        }

        if (pattern instanceof Pattern.PList list) {
            return context;
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

        if (statement instanceof Statement.ADT adt) {

            var c = context;
            var typeName = Nameable.stringOf(adt.name());
            List<Type> constructors = new ArrayList<>();
            c = c.union(Map.of(typeName, makeScheme(new Type.ADT(typeName, List.of()))));

            for (var entry : adt.typesConstructors().entrySet()) {
                var name = Nameable.stringOf(entry.getKey());
                if (entry.getValue() != null) {
                    var type = new Type.TVar(typeName);
                    var constructorType = determine(entry.getValue());
                    constructorType = new Type.TFunc(List.of(constructorType), type);
                    constructors.add(constructorType);
                    c = c.union(Map.of(name, generalize(c, constructorType)));
                } else {
                    var constructorType = new Type.TVar(typeName);
                        constructors.add(constructorType);
                    c = c.union(Map.of(name, generalize(c, constructorType)));
                }
            }

            Context finalC = c;
            var l = constructors.stream().map(type -> generalize(finalC, type)).toList();
            c = c.union(Map.of(typeName, makeScheme(new Type.ADT(typeName, l))));

            return c;
        }

        if (statement instanceof Statement.Instr) {
            return context;
        }

        if (statement instanceof Statement.Module module) {
            return infer(context, module.moduleContent());
        }

        if (statement instanceof Statement.Let let) {

            var letP = compress(let);
            var name = Nameable.stringOf(letP.name());
            var contextP = context.union(Map.of(name, makeScheme(fresh("a"))));

            var ts = infer(contextP, letP.expr());
            var tau = ts.t;
            var s0 = ts.s;

            var scheme = generalize(contextP, tau.apply(s0));

            return contextP.union(Map.of(name, scheme));
        }

        throw new RuntimeException("Missing Implementation");
    }

    public Context infer(Context context, ModuleContent content) {
        if (content == null) return new Context().union(context);
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

    public Type determine(TypeExpression expression) {
        Objects.requireNonNull(expression);

        if (expression instanceof TypeExpression.Name name) {
            return new Type.TVar(Nameable.stringOf(name.name()));
        }
        if (expression instanceof TypeExpression.Poly poly) {
            return new Type.TVar("'" + poly.identifier().id());
        }
        if (expression instanceof TypeExpression.Arrow arrow) {
            var types = arrow.types().stream().map(this::determine).toList();
            return new Type.TFunc(types.subList(1, types.size()), types.get(0));
        }
        if (expression instanceof TypeExpression.Tuple tuple) {
            return new Type.TTuple(tuple.tuple().stream().map(this::determine).toList());
        }
        if (expression instanceof TypeExpression.GetTypeModule get) {
            return new Type.TVar(Nameable.stringOf(get.typeName()));
        }
        throw new RuntimeException("Missing implementation.");
    }

}
