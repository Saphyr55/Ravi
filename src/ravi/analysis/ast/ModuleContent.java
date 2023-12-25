package ravi.analysis.ast;

public record ModuleContent(Statement statement, ModuleContent restContent) {

}
