package ravi.analysis.ast;

public sealed interface Identifier {

    record Capitalized(String name) implements Identifier { }

    record Lowercase(String name) implements Identifier { }

}
