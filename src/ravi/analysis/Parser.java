package ravi.analysis;

import ravi.analysis.ast.*;

import java.util.*;

public class Parser {

    private List<Token> tokens;
    private int position;

    public Parser() {
        this.tokens = List.of();
    }

    public Program program(List<Token> tokens) {
        this.tokens = List.copyOf(tokens);
        return program();
    }

    /**
     * Program -> Statement Program'<br>
     * Program' -> Program | epsilon
     *
     * @return program
     */
    private Program program() {
        Statement statement = statement();
        Program program = isAtEnd() ? null : program();
        return new Program(statement, program);
    }

    /**
     * Statement -> Let  | Instr  | Module
     *
     * @return statement
     */
    private Statement statement() {
        if (check(Kind.LetKw)) return let();
        if (check(Kind.ModuleKw)) return moduleStmt();
        return instructionExpr();
    }

    /**
     * Instr -> Expr ;
     *
     * @return statement
     */
    private Statement instructionExpr() {
        List<Expression> expressions = new LinkedList<>();
        expressions.add(expression());
        do {
            expressions.add(expression());
            consume(Kind.Semicolon, "We need a ';' symbole to close the instruction.");
        } while (check(Kind.Semicolon));
        return new Statement.Instr(expressions);
    }

    /**
     * Let -> let ValueName Parameters = Expr end
     *
     * @return let
     */
    private Statement.Let let() {

        consume(Kind.LetKw, "We need the 'let' keyword.");
        Nameable.ValueName valueName = valueName("We need a value name.");
        Parameters parameters = parameters();

        consume(Kind.Equal, "We need the '=' symbol.");

        Expression result = expression();
        consume(Kind.EndKw, "We need the 'end' keyword to close a let declarations.");

        return new Statement.Let(valueName, parameters, result);
    }

    /**
     * Module -> module ModuleName = ModuleContent end
     *
     * @return module
     */
    private Statement moduleStmt() {

        consume(Kind.ModuleKw, "We need the 'module' keyword.");
        Nameable.ModuleName moduleName = moduleName("We need a module name.");

        consume(Kind.Equal, "We need the '=' symbol.");

        ModuleContent content = moduleContent();
        consume(Kind.EndKw, "We need the 'end' keyword to close a let the module");

        return new Statement.Module(moduleName, content);
    }

    /**
     * ModuleContent -> ModuleContent'<br>
     * ModuleContent' -> Let ModuleContent  | epsilon
     *
     * @return module content
     */
    private ModuleContent moduleContent() {
        if (check(Kind.LetKw)) {
            Statement.Let let = let();
            return new ModuleContent(let, moduleContent());
        }
        return null;
    }

    private Expression expression() {

        Expression expression = equality();

        if (expression != null && check(Kind.Operator)) {
            Token token = consume(Kind.Operator, "");
            Expression right = expression();
            return new Expression.ApplicationOperator(
                    (String) token.value(), expression, right);
        }

        if (expression != null && check(Kind.DoubleColon)) {
            consume(Kind.DoubleColon, "");
            Expression tail = expression();
            return new Expression.ConsCell(expression, tail);
        }

        return expression;
    }

    private Expression equality() {

        Expression expr = comparison();

        var value = currentToken().text();
        var check = value.equals(Syntax.Symbol.NotEqual) ||
                value.equals(Syntax.Symbol.Equal);

        while (check(Kind.Operator) && check) {
            Operator operator = operator();
            Expression right = comparison();
            expr = new Expression.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expression comparison() {
        Expression expr = term();

        var value = currentToken().text();
        var check = value.equals(Syntax.Symbol.Greater) ||
                value.equals(Syntax.Symbol.Lower)   ||
                value.equals(Syntax.Symbol.LowerEqual) ||
                value.equals(Syntax.Symbol.GreaterEqual);

        while (check(Kind.Operator) && check) {
            Operator operator = operator();
            Expression right = term();
            expr = new Expression.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expression term() {

        Expression expr = factor();

        var value = currentToken().text();
        var check =
                value.equals(Syntax.Symbol.Minus) ||
                value.equals(Syntax.Symbol.Plus);

        while (check(Kind.Operator) && check) {
            Operator operator = operator();
            Expression right = factor();
            expr = new Expression.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expression factor() {

        Expression expr = unary();

        var value = currentToken().text();
        var check =
                value.equals(Syntax.Symbol.Slash) ||
                value.equals(Syntax.Symbol.Asterisk);

        while (check(Kind.Operator) && check) {
            Operator operator = operator();
            Expression right = unary();
            expr = new Expression.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expression unary() {

        var value = currentToken().text();
        var check =
                value.equals(Syntax.Symbol.Exclamation) ||
                value.equals(Syntax.Symbol.Minus);

        if (check(Kind.Operator) && check) {
            Operator operator = operator();
            Expression right = unary();
            return new Expression.Unary(operator, right);
        }

        return application();
    }

    /**
     * Expr -> Expr Expr'<br>
     * Expr' -> InfixOp Expr | :: Expr | epsilon
     *
     * @return expression
     */
    private Expression application() {

        Expression primary = primary();
        Expression expression = primary();

        if (expression == null) {
            return primary;
        }

        List<Expression> expressions = new LinkedList<>();

        while (expression != null) {
            expressions.add(expression);
            expression = application();
        }

        return new Expression.Application(primary, expressions);
    }

    private Expression primary() {
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
        if (check(Kind.Int)) return integerExpr();
        if (check(Kind.Float)) return floatExpr();
        return null;
    }

    /**
     * Expr -> Constant
     *
     * @return expression
     */
    private Expression floatExpr() {
        return new Expression.ConstantExpr(cFloat());
    }

    /**
     * Expr -> Constant
     *
     * @return expression
     */
    private Expression integerExpr() {
        return new Expression.ConstantExpr(integer());
    }

    /**
     * Expr -> ModuleName . ValueName
     *
     * @return expression
     */
    private Expression moduleCallExpr() {

        Nameable.ModuleName moduleName = moduleName("We need the name of the module.");
        consume(Kind.Dot, "We need the '.' symbol to call a declaration from the module.");
        Nameable.ValueName labelName = valueName("We need the name of a declaration from the module.");

        return new Expression.ModuleCallExpr(moduleName, labelName);
    }

    /**
     * Expr -> ValueName
     *
     * @return expression
     */
    private Expression valueNameExpr() {
        Nameable.ValueName labelName = valueName("We need a label name.");
        return new Expression.ValueNameExpr(labelName);
    }

    /**
     * Expr -> fun Parameters -> Expr
     *
     * @return expression
     */
    private Expression lambdaExpr() {
        consume(Kind.FunKw, "We need the 'fun' keyword to declare a lambda");
        Parameters parameters = parameters();
        consume(Kind.Arrow, "We need the '->' symbol to specifies an expression for the lambda.");
        Expression expression = expression();
        return new Expression.Lambda(parameters, expression);
    }

    /**
     * Expr -> match Expr with Pattern
     *
     * @return expression
     */
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

    /**
     * Expr -> Constant
     *
     * @return expression
     */
    private Expression textExpr() {
        return new Expression.ConstantExpr(text());
    }

    /**
     * Expr -> Constant
     *
     * @return expression
     */
    private Expression stringExpr() {
        return new Expression.ConstantExpr(string());
    }

    /**
     * Expr -> List
     *
     * @return expression
     */
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

    /**
     * RestList -> ; Expr RestList'
     * RestList' -> RestList | epsilon
     *
     * @return rest list
     */
    private RaviRestList restList() {
        if (check((Kind.CloseSquareBracket))){
            return null;
        }
        consume(Kind.Semicolon, "We need a ';' symbol.");
        Expression expr = expression();
        return new RaviRestList(expr, restList());
    }

    /**
     * Expr -> ( Expr )
     *
     * @return expression
     */
    private Expression parenthesisExpr() {
        consume(Kind.OpenParenthesis, "We need a '(' symbol.");
        if (check(Kind.Operator)) {
            var operator = valueNameOp();
            if (operator == null) throw new RuntimeException("'()' is not an operator.");
            consume(Kind.CloseParenthesis, "We need a ')' symbol.");
            return new Expression.ValueNameExpr(operator);
        }
        Expression expression = expression();
        consume(Kind.CloseParenthesis, "We need a ')' symbol.");
        if (expression == null) return new Expression.UnitExpr();
        return new Expression.ParenthesisExpr(expression);
    }

    /**
     * Expr -> begin Expr end
     *
     * @return expression
     */
    private Expression groupExpr() {
        consume(Kind.BeginKw, "We need the 'begin' keyword.");
        Expression expression = expression();
        consume(Kind.EndKw, "We need the 'end' keyword.");
        return expression;
    }

    /**
     * Expr -> let ValueName Parameters = Expr in Expr
     *
     * @return expression
     */
    private Expression letIn() {

        consume(Kind.LetKw, "We need the 'let' keyword.");
        Nameable.ValueName valueName = valueName("We need a value name.");
        Parameters parameters = parameters();

        consume(Kind.Equal, "We need the '=' symbol.");

        Expression resultLet = expression();
        consume(Kind.InKw, "We need the 'in' keyword to close a let declarations.");
        Expression resultIn = expression();

        return new Expression.LetIn(valueName, parameters, resultLet, resultIn);
    }

    /**
     * Parameters -> LabelName Parameters'
     * Parameters' -> Parameters | epsilon
     *
     * @return parameters
     */
    private Parameters parameters() {
        List<Identifier.Lowercase> identifiers = new LinkedList<>();
        while (check(Kind.LowercaseIdentifier)) {
            Token token = consume(Kind.LowercaseIdentifier, "We need an valueName.");
            Identifier.Lowercase identifier = new Identifier.Lowercase((String) token.value());
            identifiers.add(identifier);
        }
        return new Parameters(identifiers);
    }

    /**
     * Pattern -> Pattern :: Pattern | Pattern'
     *
     * @return pattern
     */
    private Pattern pattern() {
        Pattern primary = patternPrime();
        Pattern pattern = consCellPattern(primary);
        if (pattern == null) {
            return primary;
        }
        return pattern;
    }

    /**
     * Pattern -> Pattern :: Pattern
     *
     * @param head Pattern
     * @return pattern
     */
    private Pattern consCellPattern(Pattern head) {
        if (check(Kind.DoubleColon)) {
            consume(Kind.DoubleColon, "");
            Pattern tail = pattern();
            return new Pattern.PCons(head, tail);
        }
        return patternPrime();
    }

    /**
     * Pattern' -> _ | LabelName | ( Pattern ) | Constant
     *
     * @return pattern
     */
    private Pattern patternPrime() {
        if (check(Kind.LowercaseIdentifier)) return labelNamePattern();
        if (check(Kind.OpenParenthesis)) return groupPattern();
        Constant constant = constant();
        if (constant == null) return null;
        return new Pattern.PConstant(constant);
    }

    /**
     * Pattern' -> ( Pattern )
     *
     * @return pattern
     */
    private Pattern groupPattern() {
        consume(Kind.OpenParenthesis, "We need a '(' to open a group pattern.");
        Pattern pattern = pattern();
        consume(Kind.CloseParenthesis, "We need a ')' to close the group pattern.");
        return pattern;
    }

    /**
     * Pattern' -> LabelName
     *
     * @return pattern
     */
    private Pattern labelNamePattern() {
        Nameable.LabelName labelName = labelName("We need a label name.");
        if (labelName.name().name().equals("_")) {
            return new Pattern.PAny();
        }
        return new Pattern.PLabelName(labelName);
    }

    /**
     * Constant -> Int | Float | Text | String | [ ] | ( )
     *
     * @return constant
     */
    private Constant constant() {
        Token c = currentToken();
        if (check(Kind.OpenSquareBracket)) return emptyListConstant();
        if (check(Kind.Text)) return text();
        if (check(Kind.String)) return string();
        if (check(Kind.Int)) return integer();
        if (check(Kind.Float)) return cFloat();
        return null;
    }

    /**
     * Constant -> [ ]
     *
     * @return constant
     */
    private Constant emptyListConstant() {
        consume(Kind.OpenSquareBracket, "We need a '[' symbol.");
        consume(Kind.CloseSquareBracket,"We need a ']' symbol.");
        return new Constant.CEmptyList();
    }

    /**
     * Constant -> Int
     *
     * @return constant
     */
    private Constant integer() {
        Token token = consume(Kind.Int,"We need a integer.");
        return new Constant.CInt((Integer)token.value());
    }

    /**
     * Constant -> Float
     *
     * @return constant
     */
    private Constant cFloat() {
        Token token = consume(Kind.Float,"We need a float.");
        return new Constant.CFloat((Float) token.value());
    }

    /**
     * Constant -> Text
     *
     * @return constant
     */
    private Constant text() {
        Token token = consume(Kind.Text, "We need a bloc text.");
        return new Constant.CText((String) token.value());
    }

    /**
     * Constant -> String
     *
     * @return constant
     */
    private Constant string() {
        Token token = consume(Kind.String, "We need a string.");
        return new Constant.CString((String) token.value());
    }

    private Nameable.ValueName valueName(String msg) {
        if (check(Kind.OpenParenthesis)) {
            consume(Kind.OpenParenthesis, "");
            Nameable.ValueName valueName = valueNameOp();
            consume(Kind.CloseParenthesis, "We need the symbole ')' to close the value name.");
            return valueName == null ? new Nameable.ValueName.NEmpty() : valueName;
        }
        return new Nameable.ValueName.NName(lowercase(msg));
    }

    private Nameable.ValueName.NInfixOp valueNameOp() {
        Operator operator = operator();
        if (operator == null)  return null;
        return new Nameable.ValueName.NInfixOp(operator);
    }

    private Nameable.ModuleName moduleName(String msg) {
        return new Nameable.ModuleName(capitalized(msg));
    }

    private Nameable.LabelName labelName(String msg) {
        return new Nameable.LabelName(lowercase(msg));
    }

    private Identifier.Capitalized capitalized(String msg) {
        Token identifier = consume(Kind.CapitalizedIdentifier, msg);
        return new Identifier.Capitalized((String) identifier.value());
    }

    private Identifier.Lowercase lowercase(String msg) {
        Token identifier = consume(Kind.LowercaseIdentifier, msg);
        return new Identifier.Lowercase((String) identifier.value());
    }

    private Operator operator() {
        if (check(Kind.Operator)) {
            Token token = consume(Kind.Operator, "");
            return new Operator((String) token.value());
        }
        return null;
    }

    private Token consume(Kind kind, String message) {
        if (check(kind)) {
            return nextToken();
        }
        throw new RuntimeException(message);
    }

    private boolean match(Kind kind) {
        if (check(kind)) {
            nextToken();
            return true;
        }
        return false;
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
