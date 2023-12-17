package ravi.syntax;

import ravi.syntax.ast.*;

import java.util.*;

public class Parser {

    private final List<Token> tokens;
    private int position;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Program -> Statement Program | epsilon
     *
     * @return program
     */
    public Program program() {
        Statement statement = statement();
        Program program = isAtEnd() ? null : program();
        return new Program(statement, program);
    }

    private Statement statement() {
        if (check(Kind.LetKw)) return let();
        if (check(Kind.ModuleKw)) return moduleStmt();
        return instructionExpr();
    }

    private Statement instructionExpr() {
        List<Expression> expressions = new LinkedList<>();
        expressions.add(expression());
        do {
            expressions.add(expression());
            consume(Kind.Semicolon, "We need a ';' symbole to close the instruction.");
        } while (check(Kind.Semicolon));
        return new Statement.Instr(expressions);
    }

    private Statement.Let let() {

        consume(Kind.LetKw, "We need the 'let' keyword.");
        Identifier.Lowercase valueName = lowercase();
        Parameters parameters = parameters();

        consume(Kind.Equal, "We need the '=' symbol.");

        Expression result = expression();
        consume(Kind.EndKw, "We need the 'end' keyword to close a let declarations.");

        return new Statement.Let(valueName, parameters, result);
    }

    private Statement moduleStmt() {

        consume(Kind.ModuleKw, "We need the 'module' keyword.");
        Identifier.Capitalized moduleName = capitalized();

        consume(Kind.Equal, "We need the '=' symbol.");

        ModuleContent content = moduleContent();
        consume(Kind.EndKw, "We need the 'end' keyword to close a let the module");

        return new Statement.Module(moduleName, content);
    }

    private ModuleContent moduleContent() {
        if (check(Kind.LetKw)) {
            Statement.Let let = let();
            return new ModuleContent(let, moduleContent());
        }
        return null;
    }

    private Expression expression() {
        return application();
    }

    private Expression application() {

        Expression primary = expressionPrime();
        Expression expression = expressionPrime();

        if (check(Kind.DoubleColon)) {
            consume(Kind.DoubleColon, "");
            Expression tail = expression();
            return new Expression.ConsCell(primary, tail);
        }

        if (expression == null) {
            return primary;
        }

        List<Expression> expressions = new LinkedList<>();

        while (expression != null) {
            expressions.add(expression);
            expression = expression();
        }

        return new Expression.Application(primary, expressions);
    }


    private Expression expressionPrime() {
        if (check(Kind.String)) return stringExpr();
        if (check(Kind.Text)) return textExpr();
        if (check(Kind.LetKw)) return letIn();
        if (check(Kind.BeginKw)) return groupExpr();
        if (check(Kind.OpenParenthesis)) return parenthesisExpr();
        if (check(Kind.LowercaseIdentifier)) return valueNameExpr();
        if (check(Kind.OpenSquareBracket)) return listExpr();
        if (check(Kind.MatchKw)) return patternMatching();
        if (check(Kind.FunKw)) return lambdaExpr();
        if (check(Kind.CapitalizedIdentifier)) return moduleCallExpr();
        return null;
    }

    private Expression moduleCallExpr() {
        Token moduleNameToken = consume(Kind.CapitalizedIdentifier, "We need the name of the module.");
        consume(Kind.Dot, "We need the '.' symbol to call a declaration from the module.");
        Token valueName = consume(Kind.LowercaseIdentifier, "We need the name of a declaration from the module.");
        return new Expression.ModuleCallExpr(
                new Identifier.Capitalized((String) moduleNameToken.value()),
                new Identifier.Lowercase((String) valueName.value()));
    }

    private Expression valueNameExpr() {
        return new Expression.ValueNameExpr(lowercase());
    }

    private Expression lambdaExpr() {
        consume(Kind.FunKw, "We need the 'fun' keyword to declare a lambda");
        Parameters parameters = parameters();
        consume(Kind.Arrow, "We need the '->' symbol to specifies an expression for the lambda.");
        Expression expression = expression();
        return new Expression.Lambda(parameters, expression);
    }

    private Expression patternMatching() {
        consume(Kind.MatchKw, "We need the keyword 'match' to declare a pattern matching.");
        Expression expression = expression();
        consume(Kind.WithKw, "We need the keyword 'with' after an expression.");
        List<Pattern> patterns = new ArrayList<>();
        List<Expression> expressions = new ArrayList<>();
        while (check(Kind.Pipe)) {
            consume(Kind.Pipe, "");
            patterns.add(pattern());
            consume(Kind.Arrow, "We need the '->' to result an expression.");
            expressions.add(expression());
        }
        return new Expression.PatternMatching(expression, patterns, expressions);
    }

    private Expression textExpr() {
        return new Expression.ConstantExpr(text());
    }

    private Expression stringExpr() {
        return new Expression.ConstantExpr(string());
    }

    private Expression listExpr() {
        consume(Kind.OpenSquareBracket, "We need a '[' symbol.");
        if (check((Kind.CloseSquareBracket))){
            consume(Kind.CloseSquareBracket,"");
            return new Expression.ListExpr(new RaviList.EmptyList());
        }
        Expression expression = expression();
        RaviRestList rest = restList();
        consume(Kind.CloseSquareBracket, "We need a ']' symbol.");
        return new Expression.ListExpr(new RaviList.List(expression,rest));
    }

    private RaviRestList restList() {
        if (check((Kind.CloseSquareBracket))){
            return null;
        }
        consume(Kind.Semicolon, "We need a ';' symbol.");
        Expression expr = expression();
        return new RaviRestList(expr, restList());
    }

    private Expression parenthesisExpr() {
        consume(Kind.OpenParenthesis, "We need a '(' symbol.");
        Expression expression = expression();
        consume(Kind.CloseParenthesis, "We need a ')' symbol.");
        if (expression == null) return new Expression.UnitExpr();
        return new Expression.ParenthesisExpr(expression);
    }

    private Expression groupExpr() {
        consume(Kind.BeginKw, "We need the 'begin' keyword.");
        Expression expression = expression();
        consume(Kind.EndKw, "We need the 'end' keyword.");
        return expression;
    }

    private Expression letIn() {

        consume(Kind.LetKw, "We need the 'let' keyword.");
        Identifier.Lowercase valueName = lowercase();
        Parameters parameters = parameters();

        consume(Kind.Equal, "We need the '=' symbol.");

        Expression resultLet = expression();
        consume(Kind.InKw, "We need the 'in' keyword to close a let declarations.");
        Expression resultIn = expression();

        return new Expression.LetIn(valueName, parameters, resultLet, resultIn);
    }

    private Parameters parameters() {
        List<Identifier.Lowercase> identifiers = new LinkedList<>();
        while (check(Kind.LowercaseIdentifier)) {
            Token token = consume(Kind.LowercaseIdentifier, "We need an valueName.");
            Identifier.Lowercase identifier = new Identifier.Lowercase((String) token.value());
            identifiers.add(identifier);
        }
        return new Parameters(identifiers);
    }

    private Pattern pattern() {
        Pattern primary = patternPrime();
        Pattern pattern = consCellPattern(primary);
        if (pattern == null) {
            return primary;
        }
        return pattern;
    }

    private Pattern consCellPattern(Pattern head) {
        if (check(Kind.DoubleColon)) {
            consume(Kind.DoubleColon, "");
            Pattern tail = pattern();
            return new Pattern.PCons(head, tail);
        }
        return patternPrime();
    }

    private Pattern patternPrime() {
        if (check(Kind.LowercaseIdentifier)) return valueNamePattern();
        if (check(Kind.OpenParenthesis)) return groupPattern();
        Constant constant = constant();
        if (constant == null) return null;
        return new Pattern.PConstant(constant);
    }

    private Pattern groupPattern() {
        consume(Kind.OpenParenthesis, "We need a '(' to open a group pattern.");
        Pattern pattern = pattern();
        consume(Kind.CloseParenthesis, "We need a ')' to close the group pattern.");
        return pattern;
    }

    private Pattern valueNamePattern() {
        Identifier.Lowercase identifier = lowercase();
        if (identifier.name().equals("_")) {
            return new Pattern.PAny();
        }
        return new Pattern.PValueName(identifier);
    }

    private Constant constant() {
        if (check(Kind.OpenSquareBracket)) return emptyListConstant();
        if (check(Kind.Text)) return text();
        if (check(Kind.String)) return string();
        return null;
    }

    private Constant emptyListConstant() {
        consume(Kind.OpenSquareBracket, "We need a '[' symbol.");
        consume(Kind.CloseSquareBracket,"We need a ']' symbol.");
        return new Constant.CEmptyList();
    }

    private Constant text() {
        Token token = consume(Kind.Text, "We need a bloc text.");
        return new Constant.CText((String) token.value());
    }

    private Constant string() {
        Token token = consume(Kind.String, "We need a string.");
        return new Constant.CString((String) token.value());
    }

    private Identifier.Capitalized capitalized() {
        Token identifier = consume(Kind.CapitalizedIdentifier, "We need to have a moduleName capitalized.");
        return new Identifier.Capitalized((String) identifier.value());
    }

    private Identifier.Lowercase lowercase() {
        Token identifier = consume(Kind.LowercaseIdentifier, "We need to have a moduleName NOT capitalized.");
        return new Identifier.Lowercase((String) identifier.value());
    }

    private Token consume(Kind kind, String message) {
        if (check(kind)) {
            return nextToken();
        }
        throw new RuntimeException(message);
    }

    private boolean isAtEnd() {
        return check(Kind.EOF);
    }

    private Token nextToken() {

        if (position >= tokens.size()) {
            return null;
        }

        Token token = tokens.get(position);
        position++;
        return token;
    }

    private boolean check(Kind kind) {
        return currentToken().kind() == kind;
    }

    private Token currentToken() {
        return tokens.get(position);
    }

}
