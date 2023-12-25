package ravi.analysis.ast;

import java.util.List;

public interface TypeExpression {

    record Tuple(List<TypeExpression> tuple) implements TypeExpression { }

    record Name(Nameable.TypeName name) implements TypeExpression { }

    record Arrow(List<TypeExpression> types) implements TypeExpression { }

    record GetTypeModule(Nameable.ModuleName moduleName,
                         Nameable.TypeName typeName)
            implements TypeExpression { }

    record Poly(Identifier identifier) implements TypeExpression { }

}
