package ravi.model;

import java.util.Arrays;
import java.util.List;

public sealed interface Value {

    record VApplication(Application application) implements Value { }

    record VString(String content) implements Value { }

    record VNumber(Number number) implements Value { }

    record VUnit() implements Value { }

    record VObject(Object content) implements Value { }

    record VList(List<Value> values) implements Value { }


    static VUnit unit() { return new VUnit(); }

    static VApplication application(Application application) { return new VApplication(application); }

    static VObject object(Object content) { return new VObject(content); }

    static VNumber number(Number number) { return new VNumber(number); }

    static VString string(String content) { return new VString(content); }

    static VList list(List<Value> values) { return new VList(values); }


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
         if (this instanceof VNumber number) {
             return number.number.toString();
         }
         if (this instanceof VApplication) {
             return "<application>";
         }
         throw new IllegalStateException("Need to implement all values.");
    }

}
