package ravi.analysis.ast;

public record ModuleContent(Statement.Let let, ModuleContent restContent) {

}
