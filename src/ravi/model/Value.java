package ravi.model;

public sealed interface Value {

    record Func(Application application) implements Value { }

    record Str(String content) implements Value {
        @Override
        public String toString() {
            return content;
        }
    }

    record Num() implements Value { }

    record Unit() implements Value { }

    record Any(Object content) implements Value { }

}
