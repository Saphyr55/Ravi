package ravi.infer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Typing<T> {

    Set<String> ftv();

    T apply(Substitution s);

    record ListTyping<T extends Typing<?>>(Stream<T> list) implements Typing<Stream<T>> {

        public static <T extends Typing<?>> ListTyping<T> of(Stream<T> list) {
            return new ListTyping<>(list);
        }

        @Override
        public Set<String> ftv() {
            return list
                    .flatMap(t -> t.ftv().stream())
                    .collect(Collectors.toUnmodifiableSet());
        }

        @Override
        public Stream<T> apply(Substitution s) {
            return list.map(t -> (T) t.apply(s));
        }
    }

}
