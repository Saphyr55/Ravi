package ravi.core;

import ravi.model.Application;
import ravi.model.Value;
import ravi.resolver.Environment;
import ravi.resolver.Interpreter;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class NativeDeclaration {

    @RaviNative
    static Value print(Interpreter inter, Value value) {
        System.out.println(value.toStr());
        return Value.unit();
    }

    @RaviNative
    static Value addInteger(Interpreter inter, Value.VInt v1, Value.VInt v2) {
        return Value.integer(v1.integer() + v2.integer());
    }

    @RaviNative
    static Value subtractInteger(Interpreter inter, Value.VInt v1, Value.VInt v2) {
        return Value.integer(v1.integer() - v2.integer());
    }

    @RaviNative
    static Value multiplyInteger(Interpreter inter, Value.VInt v1, Value.VInt v2) {
        return Value.integer(v1.integer() * v2.integer());
    }

    @RaviNative
    static Value divideInteger(Interpreter inter, Value.VInt v1, Value.VInt v2) {
        return Value.integer(v1.integer() / v2.integer());
    }

    @RaviNative
    static Value negativeInteger(Interpreter inter, Value.VInt v1) {
        return Value.integer(-v1.integer());
    }

    @RaviNative
    static Value format(Interpreter inter, Value.VString str, Value.VList list) {
        return Value.string(String.format(str.content(),
                list.values().stream().map(Value::toStr).toArray()));
    }

    @RaviNative
    static Value concat(Interpreter inter, Value.VList v1, Value.VList v2) {
        return Value.list(Stream
                .concat(v1.values().stream(),
                        v2.values().stream())
                .toList());
    }

    /**
     *
     */
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static void genNative(Environment environment) {
        Arrays.stream(NativeDeclaration.class.getDeclaredMethods())
                .forEach(method -> genNative(environment, method));
    }

    private static void genNative(Environment environment, Method method) {
        Optional.ofNullable(method.getAnnotation(RaviNative.class)).ifPresent(aRaviNative -> genNative(method, environment, aRaviNative));
    }

    private static void genNative(Method method, Environment environment, RaviNative let) {
        Application application = (inter, args) -> call(method, inter, args);
        Value value = Application.value(method.getParameterCount() - 1, application);
        environment.define(genNameNativeLet(method, let), value);
    }

    private static Value call(Method method, Interpreter inter, List<Value> args) {
        try {
            var mt = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
            var mh = LOOKUP.findStatic(NativeDeclaration.class, method.getName(), mt);
            mh = mh.bindTo(inter);
            return (Value) mh.invokeWithArguments(args);
        } catch (Throwable e) {
            String argsName = Arrays.toString(args.stream().map(Value::toStr).toArray());
            System.err.printf("You try to apply the function '%s' with %s\n", method.getName(), argsName);
            throw new RuntimeException(e);
        }
    }

    private static String genNameNativeLet(Method method, RaviNative let) {
        if (let.name().contains(" ") || let.name().isEmpty()) {
            return method.getName();
        }
        return let.name();
     }

}
