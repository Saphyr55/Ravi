package ravi.analysis.ast;

public sealed interface Nameable {

    record LabelName(Identifier.Lowercase name) implements Nameable { }

    record ModuleName(Identifier.Capitalized name) implements Nameable { }

    record TypeName(Identifier.Capitalized name) implements Nameable { }

    record CaseName(Identifier.Capitalized name) implements Nameable { }

    sealed interface ValueName extends Nameable {

        record NName(Identifier.Lowercase name) implements ValueName { }

        record NType(Identifier.Capitalized name) implements ValueName { }

        record NInfixOp(Operator operator) implements ValueName { }

        record NEmpty() implements ValueName { }

    }

    static String stringOf(Nameable nameable) {
        if (nameable instanceof LabelName n) {
            return n.name.name();
        }
        if (nameable instanceof ModuleName n) {
            return n.name.name();
        }
        if (nameable instanceof TypeName n) {
            return n.name().name();
        }
        if (nameable instanceof CaseName n) {
            return n.name().name();
        }
        if (nameable instanceof ValueName v && v instanceof ValueName.NName n) {
            return n.name.name();
        }
        if (nameable instanceof ValueName v && v instanceof ValueName.NInfixOp o) {
            return o.operator.symbolInfixOp();
        }
        if (nameable instanceof ValueName v && v instanceof ValueName.NType t) {
            return t.name.name();
        }
        if (nameable instanceof ValueName v && v instanceof ValueName.NEmpty) {
            return "_";
        }

        throw new RuntimeException("Not all nameable derivations are implemented");
    }

}
