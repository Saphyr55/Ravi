package ravi.infer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public sealed interface Type extends Typing<Type> {

    record TUnit() implements Type { }

    record TTuple(List<Type> types) implements Type { }

    record TList(Type type) implements Type { }

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
        if (this instanceof TUnit) {
            return Set.of();
        }
        if (this instanceof TTuple tuple) {
            return tuple.types
                    .stream()
                    .flatMap(type -> type.ftv().stream())
                    .collect(Collectors.toUnmodifiableSet());
        }
        if (this instanceof TFunc f) {
            return Stream
                    .concat(f.params().stream()
                            .flatMap(type -> type.ftv().stream()),
                            f.expr.ftv().stream())
                    .collect(Collectors.toUnmodifiableSet());
        }
        if (this instanceof TList list) {
            return list.type.ftv();
        }
        if (this instanceof ADT adt) {
            return Set.of();
        }
        throw new RuntimeException("Missing implementation of Type.");
    }

    @Override
    default Type apply(Substitution s) {

        if (this instanceof TList list) {
            return new TList(list.type.apply(s));
        }

        if (this instanceof TVar var) {
            if (s.types().containsKey(var.name)) {
                return s.types().get(var.name).apply(s);
            }
            return var;
        }

        if (this instanceof TFunc func) {
            var expr = func.expr.apply(s);
            var params = func.params.stream().map(t -> t.apply(s)).toList();
            return new TFunc(params, expr);
        }

        return this;
    }

    record ADT(String typeName, List<Scheme> schemes) implements Type {}

    default String toStr() {

        if (this instanceof ADT adt) {
            return adt.typeName();
        }

        if (this instanceof TString) {
            return "String";
        }

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

        if (this instanceof TList list) {
            return "[" + list.type.toStr() + "]";
        }

        if (this instanceof TTuple tuple) {
            return String.join(" * ", tuple.types.stream().map(Type::toStr).toList());
        }

        throw new RuntimeException("Missing implementation of type");
    }


}
