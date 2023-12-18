package ravi.resolver;

import ravi.analysis.ast.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class ScopeResolver {

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
            declare(Nameable.stringOf(let.name()));
            define(Nameable.stringOf(let.name()));
            resolveLet(let.parameters(), let.result());
            return;
        }
        if (statement instanceof Statement.Instr instr) {
            instr.expression().forEach(this::resolve);
        }
        if (statement instanceof Statement.Module module) {
            declare(Nameable.stringOf(module.moduleName()));
            define(Nameable.stringOf(module.moduleName()));
            resolve(module.moduleContent());
        }
    }

    private void resolve(ModuleContent content) {
        if (content == null) return;
        resolveLet(content.let().parameters(), content.let().result());
        resolve(content.restContent());
    }

    private void resolve(Expression expression) {

        if (expression instanceof Expression.GroupExpr expr) {
            beginScope();
            resolve(expr.expr());
            endScope();
            return;
        }

        if (expression instanceof Expression.Application application) {
            resolve(application.expr());
            application.args().forEach(this::resolve);
            return;
        }

        if (expression instanceof Expression.Lambda lambda) {
            resolve(lambda.expression());
        }

        if (expression instanceof Expression.Instr expr) {
            resolve(expr.primary());
            resolve(expr.result());
            return;
        }

        if (expression instanceof Expression.ParenthesisExpr expr) {
            resolve(expr.expr());
            return;
        }

        if (expression instanceof Expression.LetIn expr) {
            declare(Nameable.stringOf(expr.valueName()));
            resolveLet(expr.parameters(), expr.expr());
            define(Nameable.stringOf(expr.valueName()));
            resolve(expr.result());
            return;
        }

        if (expression instanceof Expression.ListExpr expr) {
            resolveList(expr.list());
            return;
        }

        if (expression instanceof Expression.ConsCell application) {
            resolve(application.head());
            resolve(application.tail());
            return;
        }

    }

    private void resolveList(RaviList cons) {
        if (cons instanceof RaviList.List list) {
            resolve(list.head());
            resolveList(list.tail());
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
        if (scope.containsKey(name)) { return; }
        scope.put(name, false);
    }

    private void define(String name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name, true);
    }

    private void resolveLet(Parameters parameters, Expression result) {
        beginScope();
        for (var param : parameters.declarations()) {
            declare(param.name());
            define(param.name());
        }
        resolve(result);
        endScope();
    }

}
