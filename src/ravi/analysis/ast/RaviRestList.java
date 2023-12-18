package ravi.analysis.ast;

public record RaviRestList(Expression expression, RaviRestList rest) {
}
