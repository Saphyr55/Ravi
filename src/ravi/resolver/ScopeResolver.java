package ravi.resolver;

import ravi.syntax.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class ScopeResolver {

    private FunctionType currentFunction = FunctionType.None;
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    public ScopeResolver(Interpreter interpreter) {
        this.interpreter = interpreter;
        beginScope();
    }

    public void resolve(Program program) {
        if (program != null) {
            resolve(program.program());
            resolve(program.statement());
        }
    }

    private void resolve(Statement statement) {
        if (statement instanceof Statement.Let let) {
            declare(let.name().identifier());
            define(let.name().identifier());
            resolveFunction(let.params(), let.result(), FunctionType.Let);
            return;
        }
        if (statement instanceof Statement.Instr instr) {
            instr.expression().forEach(this::resolve);
        }
    }

    private void resolve(Expression expression) {

        if (expression instanceof Expression.IdentifierExpr expr) {
            var name = expr.identifier().identifier();
            if (!scopes.isEmpty() && scopes.peek().get(name) == Boolean.FALSE) {
                throw new ResolverException(name,
                        "Can't read local variable '%s' in its own initializer.".formatted(name));
            }
            resolveLocal(expression, name);
            return;
        }

        if (expression instanceof Expression.GroupExpr expr) {
            resolve(expr.expr());
            return;
        }

        if (expression instanceof Expression.Application application) {
            resolve(application.expr());
            application.args().forEach(this::resolve);
            return;
        }

        if (expression instanceof Expression.Instr expr) {
            resolve(expr.primary());
            resolve(expr.result());
            return;
        }

        if (expression instanceof Expression.TextExpr expr) {
            return;
        }

        if (expression instanceof Expression.ParenthesisExpr expr) {
            resolve(expr.expr());
            return;
        }

        if (expression instanceof Expression.NumberExpr expr) {

        }

        if (expression instanceof Expression.LetIn expr) {
            declare(expr.name().identifier());
            define(expr.name().identifier());
            resolveFunction(expr.params(), expr.expr(), FunctionType.Let);
            resolve(expr.result());
        }

        if (expression instanceof Expression.ListExpr expr) {
            resolveList(expr.list());
        }

    }

    private void resolveList(RaviList rlist) {
        if (rlist instanceof RaviList.List list) {
            resolve(list.expression());
            resolveList(list.rest());
        }
    }

    private void resolveList(RaviRestList rest) {
        if (rest == null)
            return;
        resolve(rest.expression());
        resolveList(rest.rest());
    }


    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void declare(String name) {

        if (scopes.isEmpty()) return;

        Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name))
            throw new ResolverException(name, "Already a variable with this name in this scope.");

        scope.put(name, false);
    }

    private void define(String name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name, true);
    }

    private void resolveLocal(Expression expression, String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                interpreter.resolve(expression, scopes.size() - 1 - i);
                return;
            }
        }
    }

    private void resolveFunction(Params params, Expression result, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for (var param : params.declarations()) {
            declare(param.identifier());
            define(param.identifier());
        }
        resolve(result);
        endScope();
        currentFunction = enclosingFunction;
    }

}
