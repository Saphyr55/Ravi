package ravi.model;

import ravi.resolver.Environment;

import java.util.Arrays;
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

    static VUnit unit() { return new VUnit(); }

    static VApplication application(Application application) { return new VApplication(application); }

    static VObject object(Object content) { return new VObject(content); }

    static VInt integer(Integer integer) { return new VInt(integer); }

    static VString string(String content) { return new VString(content); }

    static VList list(List<Value> values) { return new VList(values); }

    static VModule module(String name, Environment environment) { return new VModule(name, environment); }


    default String toStr() {
         if (this instanceof VList list) {
             return Arrays.toString(list.values.stream().map(Value::toStr).toArray());
         }
         if (this instanceof VString string) {
             return string.content;
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
         if (this instanceof VApplication) {
             return "<application>";
         }
         throw new IllegalStateException("Need to implement all values.");
    }

}
