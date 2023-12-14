package ravi.resolver;

public class ResolverException extends RuntimeException {
    public ResolverException(String name, String s) {
        super(s);
    }
}
