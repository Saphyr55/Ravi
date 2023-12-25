package ravi.model;

import ravi.resolver.Environment;

import java.util.HashMap;
import java.util.List;

public sealed interface Value {

    record VApplication(Application application) implements Value { }

    record VString(String content) implements Value { }

    record VInt(Integer integer) implements Value { }

    record VFloat(Float cFloat) implements Value { }

    record VUnit() implements Value { }

    record VObject(Object content) implements Value { }

    record VList(List<Value> values) implements Value { }

    record VModule(String name, Environment environment) implements Value { }

    record VTuple(List<Value> values) implements Value { }

    record VAlgebraicDataType(String name, Value value) implements Value { }

    static VAlgebraicDataType adt(String name, Value value) { return new VAlgebraicDataType(name, value); }

    static VTuple tuple(List<Value> values) { return new VTuple(values); }

    static VUnit unit() {
        return new VUnit();
    }

    static VApplication application(Application application) {
        return new VApplication(application);
    }

    static VObject object(Object content) {
        return new VObject(content);
    }

    static VInt integer(Integer integer) {
        return new VInt(integer);
    }

    static VString string(String content) {
        return new VString(content);
    }

    static VList list(List<Value> values) {
        return new VList(values);
    }

    static VModule module(String name, Environment environment) {
        return new VModule(name, environment);
    }


    default String toStr() {
        if (this instanceof VList list) {
            return "[" + String.join("; ", list.values.stream().map(Value::toStr).toList()) + "]";
        }
        if (this instanceof VString string) {
            return "\"" + string.content + "\"";
        }
        if (this instanceof VObject vObject) {
            return vObject.content.toString();
        }
        if (this instanceof VUnit) {
            return "()";
        }
        if (this instanceof VInt number) {
            return number.integer.toString();
        }
        if (this instanceof VFloat vFloat) {
            return vFloat.cFloat.toString();
        }
        if (this instanceof VAlgebraicDataType adt) {
            if (adt.value instanceof VUnit) return adt.name;
            return adt.name + adt.value.toStr();
        }
        if (this instanceof VApplication) {
            return "<application>";
        }
        if (this instanceof VTuple tuple) {
            return "(" + String.join(",",
                    tuple.values
                            .stream()
                            .map(Value::toStr)
                            .toList()
            ) + ")";
        }
        throw new IllegalStateException("Need to implement all values.");
    }

}
