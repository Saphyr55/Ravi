package ravi.resolver;

import ravi.model.Value;

import java.util.HashMap;

public class Environment {

    private final Environment enclosing;
    private final HashMap<String, Value> declarations;

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
        this.declarations = new HashMap<>();
    }

    public Environment() {
        this(null);
    }

    public void define(String name, Value value) {
        declarations.put(name, value);
    }

    public Value value(String name) {

        if (declarations.containsKey(name)) {
            return declarations.get(name);
        }

        if (enclosing != null) return enclosing.value(name);

        throw new InterpretException("Undefined variable '" + name + "' on get value.");
    }

    public Value search(String name) {
        Environment environment = this;
        while (environment != null) {
            Value value = environment.value(name);
            if (value != null) {
                return value;
            }
            environment = environment.enclosing;
        }
        throw new InterpretException("Undefined variable '" + name + "' on get value.");
    }

}
