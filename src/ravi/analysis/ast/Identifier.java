package ravi.analysis.ast;

public sealed interface Identifier {

    record Capitalized(String id) implements Identifier { }

    record Lowercase(String id) implements Identifier { }

    String id();

}
