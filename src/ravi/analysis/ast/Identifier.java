package ravi.analysis.ast;

public interface Identifier {

    record Capitalized(String name) { }

    record Lowercase(String name) { }

}
