package ravi.analysis;

import ravi.analysis.ast.*;

import java.util.*;

public final class Parser {

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

        if (check(Kind.LetKw)) return letStmt();
        if (check(Kind.TypeKw)) return typeStmt();
        if (check(Kind.ModuleKw)) return moduleStmt();
        var c = currentToken();
        return instructionExpr();
    }

    /**
     *
     * @return Type statement.
     */
    private Statement typeStmt() {

        consume(Kind.TypeKw, "Missing 'type' keyword.");
        List<TypeExpression.Poly> polyTypes = new ArrayList<>();

        while (check(Kind.Quote)) {
            polyTypes.add(polyTypeExpr());
        }

        Nameable.TypeName name = typeName("Missing the type id.");
        consume(Kind.Equal, "We need a '=' symbole");
        match(Kind.Pipe);

        return typeADTStmt(polyTypes, name);
    }

    /**
     *
     * @param polyTypes Poly types
     * @param name      Type id.
     * @return ADT Statement
     */
    private Statement.ADT typeADTStmt(List<TypeExpression.Poly> polyTypes, Nameable.TypeName name) {

        Map<Nameable.CaseName, TypeExpression> typesConstructors = new HashMap<>();

        do {
            TypeExpression expression = null;
            Nameable.CaseName caseName = caseName();

            if (match(Kind.OfKw)) {
                expression = typeExpr();
            } else if (match(Kind.Colon)) {
                expression = typeExpr();
            }

            typesConstructors.put(caseName, expression);
        } while (match(Kind.Pipe));

        return new Statement.ADT(polyTypes, name, typesConstructors);
    }

    /**
     *
     * @return Type expression
     */
    private TypeExpression typeExpr() {
        return typeExprPrime();
    }

    private TypeExpression typeExprPrime() {

        TypeExpression expr;

        if (check(Kind.Quote)) {

            expr = polyTypeExpr();

        } else if(match(Kind.OpenParenthesis)) {

            TypeExpression expression = typeExprPrime();
            consume(Kind.CloseParenthesis, "We need to close the parenthesis.");
            expr = expression;
        } else if (check(Kind.CapitalizedIdentifier)) {

            var name = capitalized("We need a type name.");
            expr = check(Kind.Dot)
                    ? typeModuleExpr(new Nameable.ModuleName(name))
                    : new TypeExpression.Name(new Nameable.TypeName(name));
        } else {

            expr = typeExpr();
        }

        if (check(Kind.Operator) && currentToken().value().equals(Token.Symbol.Asterisk)) {
            return tupleTypeExpr(expr);
        }

        if (check(Kind.Arrow)) {
            return arrowTypeExpr(expr);
        }

        return expr;
    }

    /**
     *
     * @return Type expression
     */
    private TypeExpression.Poly polyTypeExpr() {
        consume(Kind.Quote, "We need a quote symbol to precise a poly type.");
        return new TypeExpression.Poly(identifier());
    }

    /**
     *
     * @param moduleName Module id
     * @return Type expression
     */
    private TypeExpression.GetTypeModule typeModuleExpr(Nameable.ModuleName moduleName) {
        consume(Kind.Dot, "We need a '.' to call");
        Nameable.TypeName typeName = typeName("We need a type id.");
        return new TypeExpression.GetTypeModule(moduleName, typeName);
    }

    /**
     *
     * @param expression Type expression
     * @return Type expression
     */
    private TypeExpression.Arrow arrowTypeExpr(TypeExpression expression) {
        List<TypeExpression> tuple = new ArrayList<>();
        tuple.add(expression);
        while (match(Kind.Arrow)) {
            tuple.add(typeExpr());
        }
        return new TypeExpression.Arrow(tuple);
    }

    /**
     *
     * @param expression Type expression
     * @return Type expression
     */
    private TypeExpression.Tuple tupleTypeExpr(TypeExpression expression) {
        List<TypeExpression> tuple = new ArrayList<>();
        tuple.add(expression);
        Token c = currentToken();
        while (c.text().equals(Token.Symbol.Asterisk) && match(Kind.Operator)) {
            tuple.add(typeExpr());
            c = currentToken();
        }
        return new TypeExpression.Tuple(tuple);
    }

    /**
     * Instr -> Expr ;
     *
     * @return statement
     */
    private Statement.Instr instructionExpr() {
        List<Expression> expressions = new LinkedList<>();
        expressions.add(expression());
        do {
            expressions.add(expression());
            consume(Kind.Semicolon, "We need a ';' symbole to close the instruction.");
        } while (check(Kind.Semicolon));
        return new Statement.Instr(expressions);
    }

    /**
     * Let -> statement ValueName Parameters = Expr end
     *
     * @return statement
     */
    private Statement.Let letStmt() {

        consume(Kind.LetKw, "We need the 'statement' keyword.");
        Nameable.ValueName valueName = valueName("We need a value id.");
        Parameters parameters = parameters();

        consume(Kind.Equal, "We need the '=' symbol.");

        Expression result = expression();
        consume(Kind.EndKw, "We need the 'end' keyword to close a statement declarations.");

        return new Statement.Let(valueName, parameters, result);
    }

    /**
     * Module -> module ModuleName = ModuleContent end
     *
     * @return module
     */
    private Statement.Module moduleStmt() {

        consume(Kind.ModuleKw, "We need the 'module' keyword.");
        Nameable.ModuleName moduleName = moduleName();

        consume(Kind.Equal, "We need the '=' symbol.");

        ModuleContent content = moduleContent();
        consume(Kind.EndKw, "We need the 'end' keyword to close a statement the module");

        return new Statement.Module(moduleName, content);
    }

    /**
     * ModuleContent -> ModuleContent'<br>
     * ModuleContent' -> Let ModuleContent | epsilon
     *
     * @return module statement
     */
    private ModuleContent moduleContent() {
        if (check(Kind.LetKw)) {
            Statement.Let letStmt = letStmt();
            return new ModuleContent(letStmt, moduleContent());
        }
        if (check(Kind.TypeKw)) {
            Statement typeStmt = typeStmt();
            return new ModuleContent(typeStmt, moduleContent());
        }
        return null;
    }

    private Expression expression() {

        Expression expression = equality();

        if (expression != null && check(Kind.Operator)) {
            Token token = consume(Kind.Operator, "");
            Expression right = expression();
            return new Expression.ApplicationOperator(
                    right, (String) token.value(), expression);
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
        var check = value.equals(Token.Symbol.NotEqual) ||
                value.equals(Token.Symbol.Equal);

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
        var check = value.equals(Token.Symbol.Greater) ||
                value.equals(Token.Symbol.Lower)   ||
                value.equals(Token.Symbol.LowerEqual) ||
                value.equals(Token.Symbol.GreaterEqual);

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
                value.equals(Token.Symbol.Minus) ||
                value.equals(Token.Symbol.Plus);

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
                value.equals(Token.Symbol.Slash) ||
                value.equals(Token.Symbol.Asterisk);

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
                value.equals(Token.Symbol.Exclamation) ||
                value.equals(Token.Symbol.Minus);

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
     * @return expr
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
            expression = primary();
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
     * @return expr
     */
    private Expression floatExpr() {
        return new Expression.ConstantExpr(cFloat());
    }

    /**
     * Expr -> Constant
     *
     * @return expr
     */
    private Expression integerExpr() {
        return new Expression.ConstantExpr(integer());
    }

    /**
     * Expr -> ModuleName . ValueName
     *
     * @return expr
     */
    private Expression moduleCallExpr() {
        Identifier.Capitalized identifier = capitalized("We need the a capitalized id.");
        if (match(Kind.Dot)) {
            Nameable.ValueName labelName = valueName("We need a value id of a declaration from the module.");
            return new Expression.ModuleCallExpr(new Nameable.ModuleName(identifier), labelName);
        }
        return new Expression.IdentExpr(new Nameable.ValueName.NType(identifier));
    }

    /**
     * Expr -> ValueName
     *
     * @return expr
     */
    private Expression valueNameExpr() {
        Nameable.ValueName labelName = valueName("We need a label id.");
        return new Expression.IdentExpr(labelName);
    }

    /**
     * Expr -> fun Parameters -> Expr
     *
     * @return expr
     */
    private Expression lambdaExpr() {
        consume(Kind.FunKw, "We need the 'fun' keyword to declare a lambda");
        Parameters parameters = parameters();
        consume(Kind.Arrow, "We need the '->' symbol to specifies an expr for the lambda.");
        Expression expression = expression();
        return new Expression.Lambda(parameters, expression);
    }

    /**
     * Expr -> match Expr with Pattern
     *
     * @return expr
     */
    private Expression patternMatching() {
        consume(Kind.MatchKw, "We need the keyword 'match' to declare a pattern matching.");
        Expression expression = expression();
        consume(Kind.WithKw, "We need the keyword 'with' after an expr.");
        List<Pattern> patterns = new ArrayList<>();
        List<Expression> expressions = new ArrayList<>();
        while (check(Kind.Pipe)) {
            consume(Kind.Pipe, "");
            patterns.add(pattern());
            consume(Kind.Arrow, "We need the '->' to expr an expr.");
            expressions.add(expression());
        }
        return new Expression.PatternMatching(expression, patterns, expressions);
    }

    /**
     * Expr -> Constant
     *
     * @return expr
     */
    private Expression textExpr() {
        return new Expression.ConstantExpr(text());
    }

    /**
     * Expr -> Constant
     *
     * @return expr
     */
    private Expression stringExpr() {
        return new Expression.ConstantExpr(string());
    }

    /**
     * Expr -> List
     *
     * @return expr
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
        if (check((Kind.CloseSquareBracket))) {
            return null;
        }
        consume(Kind.Semicolon, "We need a ';' symbol.");
        Expression expr = expression();
        return new RaviRestList(expr, restList());
    }

    /**
     * Expr -> ( Expr )
     *
     * @return expr
     */
    private Expression parenthesisExpr() {

        consume(Kind.OpenParenthesis, "We need a '(' symbol.");
        if (check(Kind.Operator)) {
            return operatorExpr();
        }
        Expression expression = expression();
        if (check(Kind.Comma)) {
            return tupleExpr(expression);
        }
        consume(Kind.CloseParenthesis, "We need a ')' symbol.");
        if (expression == null) {
            return new Expression.UnitExpr();
        }
        return new Expression.ParenthesisExpr(expression);
    }

    private Expression operatorExpr() {

        var operator = valueNameOp();
        if (operator == null) throw new RuntimeException("'()' is not an operator.");
        consume(Kind.CloseParenthesis, "We need a ')' symbol.");
        return new Expression.IdentExpr(operator);
    }

    private Expression tupleExpr(Expression expression) {

        List<Expression> expressions = new ArrayList<>();
        expressions.add(expression);

        while (match(Kind.Comma)) {
            Expression expr = expression();
            expressions.add(expr);
        }

        consume(Kind.CloseParenthesis, "We need to close parenthesis.");
        return new Expression.Tuple(expressions);
    }

    /**
     * Expr -> begin Expr end
     *
     * @return expr
     */
    private Expression groupExpr() {

        consume(Kind.BeginKw, "We need the 'begin' keyword.");
        Expression expression = expression();
        consume(Kind.EndKw, "We need the 'end' keyword.");
        return expression;
    }

    /**
     * Expr -> statement ValueName Parameters = Expr in Expr
     *
     * @return expr
     */
    private Expression letIn() {

        consume(Kind.LetKw, "We need the 'statement' keyword.");
        Nameable.ValueName valueName = valueName("We need a value id.");
        Parameters parameters = parameters();

        consume(Kind.Equal, "We need the '=' symbol.");

        Expression resultLet = expression();
        consume(Kind.InKw, "We need the 'in' keyword to close a statement declarations.");
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

        List<Nameable.LabelName> labelNames = new LinkedList<>();

        while (check(Kind.LowercaseIdentifier)) {
            Nameable.LabelName labelName = labelName("We need an id.");
            labelNames.add(labelName);
        }

        return new Parameters(labelNames);
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

        if (check(Kind.CapitalizedIdentifier)) return adtPattern();
        if (check(Kind.LowercaseIdentifier)) return labelNamePattern();
        if (check(Kind.OpenParenthesis)) return tuplePattern();

        Constant constant = constant();

        if (constant == null) return null;

        return new Pattern.PConstant(constant);
    }

    private Pattern adtPattern() {

        Nameable.CaseName name = caseName();

        if (check(Kind.OpenParenthesis)) {
            Pattern.PTuple tuplePattern = tuplePattern();
            return new Pattern.PAdt(name, tuplePattern);
        }

        return new Pattern.PAdt(name, patternPrime());
    }

    /**
     * Pattern' -> ( Pattern )
     *
     * @return pattern
     */
    private Pattern.PTuple tuplePattern() {

        consume(Kind.OpenParenthesis, "We need a '(' to open a group pattern.");
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(pattern());

        while (match(Kind.Comma)) {
            patterns.add(pattern());
        }

        consume(Kind.CloseParenthesis, "We need a ')' to close the group pattern.");
        return new Pattern.PTuple(patterns);
    }

    /**
     * Pattern' -> LabelName
     *
     * @return pattern
     */
    private Pattern labelNamePattern() {

        Nameable.LabelName labelName = labelName("We need a label id.");

        if (labelName.name().id().equals("_")) {
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

        if (match(Kind.OpenParenthesis)) {
            Nameable.ValueName valueName = valueNameOp();
            consume(Kind.CloseParenthesis, "We need the symbole ')' to close the value id.");
            return valueName == null ? new Nameable.ValueName.NEmpty() : valueName;
        }

        if (check(Kind.CapitalizedIdentifier)) {
            return new Nameable.ValueName.NType(capitalized(msg));
        }

        return new Nameable.ValueName.NName(lowercase(msg));
    }

    /**
     *
     * @return Infix operator
     */
    private Nameable.ValueName.NInfixOp valueNameOp() {

        Operator operator = operator();

        if (operator == null) {
            return null;
        }

        return new Nameable.ValueName.NInfixOp(operator);
    }

    /**
     * @return Module id
     */
    private Nameable.ModuleName moduleName() {
        return new Nameable.ModuleName(capitalized("We need a module id."));
    }

    /**
     *
     * @param msg Error message
     * @return Type id
     */
    private Nameable.TypeName typeName(String msg) {
        return new Nameable.TypeName(capitalized(msg));
    }

    /**
     * @return Case id
     */
    private Nameable.CaseName caseName() {
        return new Nameable.CaseName(capitalized("We need a case id."));
    }

    /**
     *
     * @param msg Error message
     * @return Label
     */
    private Nameable.LabelName labelName(String msg) {
        return new Nameable.LabelName(lowercase(msg));
    }

    /**
     *
     * @param msg Error message
     * @return Capitalized identifier
     */
    private Identifier.Capitalized capitalized(String msg) {

        Token identifier = consume(Kind.CapitalizedIdentifier, msg);
        return new Identifier.Capitalized((String) identifier.value());
    }

    /**
     *
     * @param msg Error message
     * @return Lowercase identifier
     */
    private Identifier.Lowercase lowercase(String msg) {

        Token identifier = consume(Kind.LowercaseIdentifier, msg);
        return new Identifier.Lowercase((String) identifier.value());
    }

    /**
     *
     * @return Identifier
     */
    private Identifier identifier() {

        String message = "We need an identifier.";
        if (check(Kind.LowercaseIdentifier)) return lowercase(message);
        if (check(Kind.CapitalizedIdentifier)) return capitalized(message);

        throw new RuntimeException(message);
    }


    /**
     *
     * @return Operator
     */
    private Operator operator() {

        if (check(Kind.Operator)) {
            Token token = consume(Kind.Operator, "");
            return new Operator((String) token.value());
        }

        return null;
    }

    /**
     *
     * @param kind Token type
     * @param message Error message
     * @return Matching token
     */
    private Token consume(Kind kind, String message) {

        if (check(kind)) {
            return nextToken();
        }

        throw new RuntimeException(
                "[Line: %d] Error : %s".formatted(
                    currentToken().line(),
                    message
            ));
    }

    /**
     *
     * @param kind Token type
     * @return matching
     */
    private boolean match(Kind kind) {

        if (check(kind)) {
            nextToken();
            return true;
        }

        return false;
    }

    /**
     *
     * @return Is at end
     */
    private boolean isAtEnd() {
        return check(Kind.EOF);
    }

    /**
     *
     * @return Next token
     */
    private Token nextToken() {

        if (position >= tokens.size()) {
            return null;
        }

        Token token = tokens.get(position);
        position++;
        return token;
    }

    /**
     *
     * @param kind Token kind
     * @return boolean
     */
    private boolean check(Kind kind) {
        return currentToken().kind() == kind;
    }

    /**
     *
     * @return Current token
     */
    private Token currentToken() {
        return tokens.get(position);
    }


}
