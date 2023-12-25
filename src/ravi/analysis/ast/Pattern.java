package ravi.analysis.ast;

import java.util.List;

public sealed interface Pattern {

    record PAny() implements Pattern { }

    record PConstant(Constant constant) implements Pattern { }

    record PCons(Pattern head, Pattern tail) implements Pattern { }

    record PLabelName(Nameable.LabelName labelName) implements Pattern { }

    record PTuple(List<Pattern> patterns) implements Pattern { }

    record PList(List<Pattern> patterns) implements Pattern { }

    record PAdt(Nameable.CaseName name, Pattern pattern) implements Pattern { }

}
