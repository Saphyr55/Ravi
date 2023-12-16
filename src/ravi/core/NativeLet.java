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

public final class NativeLet {

    @RaviNativeLet
    static Value print(Interpreter inter, Value value) {
        System.out.println(value.toStr());
        return Value.unit();
    }

    @RaviNativeLet
    static Value format(Interpreter inter, Value.VString str, Value.VList list) {
        return Value.string(String.format(str.content(),
                list.values().stream()
                .map(Value::toStr)
                .toArray()));
    }

    @RaviNativeLet
    static Value concat(Interpreter inter, Value.VList v1, Value.VList v2) {
        return Value.list(Stream
                .concat(v1.values().stream(),
                        v2.values().stream())
                .toList());
    }

    public static void genNative(Environment environment) {
        Arrays.stream(NativeLet.class.getDeclaredMethods())
                .forEach(method -> genNative(environment, method));
    }

    private static void genNative(Environment environment, Method method) {
        Optional.ofNullable(method.getAnnotation(RaviNativeLet.class)).ifPresent(raviNativeLet -> genNative(method, environment, raviNativeLet));
    }

    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static void genNative(Method method, Environment environment, RaviNativeLet let) {
        Application application = (inter, args) -> call(method, inter, args);
        Value value = Application.value(method.getParameterCount() + 1, application);
        environment.define(genNameNativeLet(method, let), value);
    }

    private static Value call(Method method, Interpreter inter, List<?> args) {
        try {
            var mt = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
            var mh = LOOKUP.findStatic(NativeLet.class, method.getName(), mt);
            mh = mh.bindTo(inter);
            return (Value) mh.invokeWithArguments(args);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static String genNameNativeLet(Method method, RaviNativeLet let) {
        if (let.name().contains(" ") || let.name().isEmpty()) {
            return method.getName();
        }
        return let.name();
     }

}
