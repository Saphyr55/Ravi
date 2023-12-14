package ravi.model;


import ravi.resolver.Interpreter;

import java.util.List;

public interface Application {

    Value apply(Interpreter inter, List<Value> args);

    default int arity() { return 0; }

}
