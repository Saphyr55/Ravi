package ravi.syntax.ast;

public sealed interface Pattern {

    record PAny() implements Pattern { }

    record PConstant(Constant constant) implements Pattern { }

    record PCons(Pattern head, Pattern tail) implements Pattern { }

    record PGroup(Pattern inner) implements Pattern { }

    record PValueName(Identifier.Lowercase identifier) implements Pattern { }


}
