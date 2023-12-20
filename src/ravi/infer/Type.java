package ravi.infer;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public sealed interface Type extends Typing<Type> {

    record TUnit() implements Type { }

    record TList() implements Type { }

    record TFunc(List<Type> params, Type expr) implements Type { }

    record TInt() implements Type { }

    record TFloat() implements Type { }

    record TString() implements Type { }

    record TVar(String name) implements Type { }

    @Override
    default Set<String> ftv() {
        if (this instanceof TVar var) {
            return Set.of(var.name);
        }
        if (this instanceof TInt) {
            return Set.of();
        }
        if (this instanceof TFloat) {
            return Set.of();
        }
        if (this instanceof TString) {
            return Set.of();
        }
        if (this instanceof TFunc f) {
            var x = new HashSet<>(f.expr.ftv());
            x.addAll(f.params.get(0).ftv());
            return x;
        }
        throw new RuntimeException("Missing implementation of Type.");
    }

    @Override
    default Type apply(Substitution s) {

        if (this instanceof TVar var) {
            return s.types().getOrDefault(var.name, new TVar(var.name));
        }

        if (this instanceof TFunc func) {
            var param = func.params.get(0).apply(s);
            var expr = func.expr.apply(s);
            return new TFunc(Collections.singletonList(param), expr);
        }

        return this;
    }

    default String toStr() {

        if (this instanceof TUnit) {
            return "Unit";
        }

        if (this instanceof TFloat) {
            return "Float";
        }

        if (this instanceof TInt) {
            return "Int";
        }

        if (this instanceof TVar var) {
            return var.name;
        }

        if (this instanceof TFunc t) {
            return "( "
                    + String.join(" -> ", t.params.stream().map(Type::toStr).toList())
                    + " -> "
                    + t.expr.toStr()
                    + " )";
        }

        if (this instanceof TList) {
            return "[]";
        }

        throw new RuntimeException("Missing implementation of type");
    }

}
