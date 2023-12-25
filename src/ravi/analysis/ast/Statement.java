package ravi.analysis.ast;


import java.util.List;
import java.util.Map;

public sealed interface Statement {

    record Let(Nameable.ValueName name,
               Parameters parameters,
               Expression expr)
            implements Statement { }

    record Instr(List<Expression> expression)
            implements Statement { }

    record Module(Nameable.ModuleName moduleName,
                  ModuleContent moduleContent)
            implements Statement { }

    record TypeADTStatement(Nameable.TypeName name,
                            Map<Nameable.CaseName, TypeExpression> typesConstructors)
            implements Statement { }


}
