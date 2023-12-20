package ravi.infer;

import java.util.*;
import java.util.stream.Collectors;

public record Context(Map<String, Scheme> env) implements Typing<Context> {

    public Context(Map<String, Scheme> env) {
        this.env = Map.copyOf(env);
    }

    public Context() {
        this(Map.of());
    }

    public Context remove(String var) {
        var m = new HashMap<>(env);
        m.remove(var);
        return new Context(m);
    }

    public Context removeAll(Collection<String> vars) {
        var m = new HashMap<>(env);
        vars.forEach(m::remove);
        return new Context(m);
    }

    public Context union(Map<String, Scheme> env) {
        var m = new HashMap<>(this.env);
        m.putAll(env);
        return new Context(m);
    }

    @Override
    public Set<String> ftv() {
        return env.values().stream().flatMap(scheme -> scheme.type().ftv().stream()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Context apply(Substitution s) {
        Map<String, Scheme> env = new HashMap<>();
        this.env.forEach((key, value) -> env.put(key, value.apply(s)));
        return new Context(env);
    }

    @Override
    public String toString() {
        return String.join("\n", env.entrySet().stream().map(e ->
                "val " + e.getKey() + " : " + e.getValue()).toList());
    }
}
