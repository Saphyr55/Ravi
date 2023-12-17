package ravi.syntax.model;

public sealed interface RaviList{

    record EmptyList() implements RaviList { }

    record List(Expression head, RaviRestList tail) implements RaviList { }

}
