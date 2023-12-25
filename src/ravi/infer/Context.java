package ravi.infer;

import ravi.model.Value;
import ravi.resolver.InterpretException;

import java.util.*;
import java.util.stream.Collectors;

public record Context(Map<String, Scheme> env, Context enclosing) implements Typing<Context> {

    public Context(Map<String, Scheme> env, Context enclosing) {
        this.env = Map.copyOf(env);
        this.enclosing = enclosing;
    }

    public Context() {
        this(Map.of(), null);
    }

    public Context remove(String var) {
        var m = new HashMap<>(env);
        m.remove(var);
        return new Context(m, enclosing);
    }

    public Context removeAll(Collection<String> vars) {
        var m = new HashMap<>(env);
        vars.forEach(m::remove);
        return new Context(m, enclosing);
    }

    public Context union(Map<String, Scheme> env) {
        var m = new HashMap<>(this.env);
        m.putAll(env);
        return new Context(m, enclosing);
    }

    public Context union(Context context) {
        return union(context.env);
    }

    public Scheme get(String valueName) {

        if (env.containsKey(valueName)) {
            return env.get(valueName);
        }

        throw new RuntimeException("Undefined type '" + valueName + "' on get moduleName.");
    }

    public Scheme value(String name) {

        if (env.containsKey(name)) {
            return env.get(name);
        }

        if (enclosing != null) return enclosing.value(name);

        throw new RuntimeException("Undefined type '" + name + "' on get value id.");
    }

    public Scheme search(String name) {
        Context context = this;
        while (context != null) {
            Scheme value = context.value(name);
            if (value != null) {
                return value;
            }
            context = context.enclosing;
        }
        throw new RuntimeException("Undefined type '" + name + "' on get id.");
    }

    @Override
    public Set<String> ftv() {
        return env
                .values()
                .stream()
                .flatMap(scheme -> scheme.type().ftv().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Context apply(Substitution s) {
        Map<String, Scheme> env = new HashMap<>();
        this.env.forEach((key, value) -> env.put(key, value.apply(s)));
        Context c = enclosing;
        if (c != null) c = c.apply(s);
        return new Context(env, c);
    }

    @Override
    public String toString() {
        return String.join("\n", env.entrySet().stream().map(e ->
                "val " + e.getKey() + " : " + e.getValue()).toList());
    }
}
