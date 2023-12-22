package ravi.infer;


import java.util.Set;

public interface Typing<T> {

    Set<String> ftv();

    T apply(Substitution s);

}
