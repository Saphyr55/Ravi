package tla.syntax;

import static tla.core.Core.stringMax;

public record Token(Kind kind, Object value, String text, int line, int col) {

    @Override
    public String toString() {
        return "Token{" +
                "kind=" + kind +
                ", value=" + (value != null && value instanceof String ? stringMax((String) value, 20) : value) +
                ", text='" + stringMax(text, 20) + '\'' +
                ", line=" + line +
                ", col=" + col +
                '}';
    }

}
