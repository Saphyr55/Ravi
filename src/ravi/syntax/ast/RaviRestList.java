package ravi.syntax.ast;

public record RaviRestList(Expression expression, RaviRestList rest) {
}
