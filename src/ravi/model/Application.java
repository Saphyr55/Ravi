package ravi.model;


import ravi.resolver.Interpreter;

import java.util.List;

public interface Application {

    Value apply(Interpreter inter, List<Value> args);

    default int arity() { return 0; }

    static Value.VApplication value(int arity, Application application) {
        return new Value.VApplication(new Application() {
            @Override
            public Value apply(Interpreter inter, List<Value> args) {
                return application.apply(inter, args);
            }

            @Override
            public int arity() {
                return arity;
            }
        });
    }

}
