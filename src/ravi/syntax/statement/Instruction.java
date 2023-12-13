package ravi.syntax.statement;

import java.util.List;

public sealed interface Instruction extends Statement {

    record Let(Identifier name, List<Identifier> args) implements Instruction { }

}
