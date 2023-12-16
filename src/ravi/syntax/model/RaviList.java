package ravi.syntax.model;

public sealed interface RaviList{

    record EmptyList() implements RaviList { }

    record List(Expression expression, RaviRestList rest) implements RaviList { }

}
