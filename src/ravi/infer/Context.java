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

    public Context union(Context context) {
        return union(context.env);
    }

    public Context union(Map<String, Scheme> env) {
        var m = new HashMap<>(this.env);
        m.putAll(env);
        return new Context(m);
    }

    @Override
    public Set<String> ftv() {
        return Typing.ListTyping.of(env.values().stream()).ftv();
    }

    @Override
    public Context apply(Substitution s) {
        Map<String, Scheme> env = new HashMap<>();
        this.env.forEach((key, value) -> env.put(key, value.apply(s)));
        return new Context(env);
    }

    Scheme generalize(Type t) {

        Set<String> ftvT = t.ftv();
        Set<String> ftvEnv = Set.copyOf(env.keySet());

        // Set difference: ftv t \ ftv env
        Set<String> vars = ftvT.stream()
                .filter(variable -> !ftvEnv.contains(variable))
                .collect(Collectors.toSet());

        // Construct Scheme with the list of variables
        return new Scheme(List.copyOf(vars), t);
    }

}
