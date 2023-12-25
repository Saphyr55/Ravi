package ravi.analysis.ast;

import java.util.List;

public interface TypeExpression {

    record TupleType(List<TypeExpression> tuple) implements TypeExpression { }

    record NameType(Nameable.TypeName name) implements TypeExpression { }

}
