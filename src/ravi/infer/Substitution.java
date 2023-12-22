package ravi.infer;

import java.util.HashMap;
import java.util.Map;

public record Substitution(Map<String, Type> types) {

    public Substitution(Map<String, Type> types) {
        this.types = Map.copyOf(types);
    }

    public Substitution(Substitution s) {
        this(s.types);
    }

    public static Substitution empty() {
        return new Substitution(Map.of());
    }

    Substitution compose(Substitution s) {
        var ts = new HashMap<>(types);
        ts.putAll(s.types);
        ts.forEach((s1, type) -> type.apply(this));
        return new Substitution(ts);
    }


}
