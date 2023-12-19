package ravi.infer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public sealed interface Type extends Typing<Type> {

    record TUnit() implements Type { }

    record TList() implements Type { }

    record TFunc(Type expr, List<Type> params) implements Type { }

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
            return Stream
                    .concat(f.expr.ftv().stream(), Typing.ListTyping.of(f.params.stream()).ftv().stream())
                    .collect(Collectors.toUnmodifiableSet());
        }
        throw new RuntimeException("Missing implementation of Type.");
    }

    @Override
    default Type apply(Substitution s) {
        if (this instanceof TVar var) {
            return s.types().getOrDefault(var.name, new TVar(var.name));
        }
        if (this instanceof TFunc func) {
            return new TFunc(func.expr.apply(s), Typing.ListTyping.of(func.params.stream()).apply(s).toList());
        }
        return this;
    }



}
