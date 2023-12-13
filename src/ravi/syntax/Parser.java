package ravi.syntax;

import ravi.syntax.statement.Program;

import java.util.List;

public class Parser {


    private List<Token> tokens;


    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Program program() {
        return new Program();
    }

}
