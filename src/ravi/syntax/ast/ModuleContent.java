package ravi.syntax.ast;

public record ModuleContent(Statement.Let let, ModuleContent restContent) {

}
